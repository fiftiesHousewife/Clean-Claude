package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonObject;
import org.fiftieshousewife.cleancode.refactoring.extractmethod.ExtractMethodRecipe;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP adapter over {@link ExtractMethodRecipe}. Parses the target file,
 * runs the recipe in-process, writes the result back on success, and
 * returns a {@link ToolResult} — no Gradle, no ProcessBuilder, no stdout
 * noise. The recipe's rejection reason (when applicable) is the text
 * payload of the error result so the agent can decide whether to widen
 * the range, retry, or give up.
 */
public final class ExtractMethodTool implements Tool {

    private static final String NAME = "extract_method";
    private static final String FILE = "file";
    private static final String START_LINE = "startLine";
    private static final String END_LINE = "endLine";
    private static final String NEW_METHOD_NAME = "newMethodName";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Extract a contiguous range of top-level statements from a Java method into a new "
                + "package-private helper. Rejects when the range contains break/continue/non-bare "
                + "return, when it would need more than one output variable, or when the selection "
                + "doesn't map cleanly to a statement range. See docs/extract-method-recipe.md.";
    }

    @Override
    public JsonObject inputSchema() {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        final JsonObject properties = new JsonObject();
        properties.add(FILE, property("string",
                "Path to the target Java file, absolute or relative to the server's working directory."));
        properties.add(START_LINE, property("integer",
                "1-based inclusive line number where the extraction range begins."));
        properties.add(END_LINE, property("integer",
                "1-based inclusive line number where the extraction range ends."));
        properties.add(NEW_METHOD_NAME, property("string",
                "Name for the new helper method (Java identifier)."));
        schema.add("properties", properties);
        final var required = new com.google.gson.JsonArray();
        required.add(FILE);
        required.add(START_LINE);
        required.add(END_LINE);
        required.add(NEW_METHOD_NAME);
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolResult call(final Map<String, Object> arguments) {
        try {
            return runExtract(
                    Path.of(requireString(arguments, FILE)),
                    requireInt(arguments, START_LINE),
                    requireInt(arguments, END_LINE),
                    requireString(arguments, NEW_METHOD_NAME));
        } catch (IllegalArgumentException e) {
            return ToolResult.error("bad arguments: " + e.getMessage());
        } catch (IOException e) {
            return ToolResult.error("io error: " + e.getMessage());
        }
    }

    // OpenRewrite flag (see org.openrewrite.Parser.requirePrintEqualsInput) that
    // gates a self-check comparing the printed AST to the input bytes. We've
    // observed false positives on valid Javadoc containing em-dashes and
    // {@link} references — printAll() produces byte-identical output but the
    // check's own comparison path reports a mismatch and returns a ParseError.
    // We disable it and rely on {@link #parsesCleanly} to guarantee we never
    // write syntactically broken output.
    private static final String REQUIRE_PRINT_EQUALS_INPUT = "org.openrewrite.requirePrintEqualsInput";

    private static ToolResult runExtract(final Path file, final int startLine, final int endLine,
                                         final String newMethodName) throws IOException {
        final String source = Files.readString(file);
        final ExtractMethodRecipe recipe = new ExtractMethodRecipe(
                file.getFileName().toString(), startLine, endLine, newMethodName);
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        final Parser.Input input = Parser.Input.fromString(file, source);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .build()
                .parseInputs(List.of(input), null, ctx)
                .toList();
        final Optional<String> parseError = firstParseError(parsed);
        if (parseError.isPresent()) {
            return ToolResult.error("extract_method could not parse file: " + parseError.get());
        }
        if (parsed.isEmpty() || !(parsed.getFirst() instanceof J.CompilationUnit cu)) {
            return ToolResult.error("extract_method: parser did not produce a CompilationUnit");
        }
        final Optional<String> after = recipe.extractTextually(cu, source, new Cursor(null, cu));
        if (after.isEmpty()) {
            return ToolResult.error("extract_method rejected: " + recipe.lastRejectionReason()
                    .orElse("range did not produce any change"));
        }
        if (!parsesCleanly(file, after.get())) {
            return ToolResult.error("extract_method aborted: the rewritten source did not re-parse "
                    + "— refusing to write a file that would not compile");
        }
        final String newSource = after.get();
        Files.writeString(file, newSource);
        return ToolResult.ok("extracted " + newMethodName + " from lines "
                + startLine + "-" + endLine + "; file " + countLines(source) + " -> "
                + countLines(newSource) + " lines "
                + "(new helper appended after the enclosing method; earlier line numbers unchanged)");
    }

    static int countLines(final String source) {
        if (source.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < source.length() - 1; i++) {
            if (source.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static Optional<String> firstParseError(final List<SourceFile> parsed) {
        return parsed.stream()
                .filter(s -> s instanceof org.openrewrite.tree.ParseError)
                .map(s -> ((org.openrewrite.tree.ParseError) s).getMarkers()
                        .findFirst(org.openrewrite.ParseExceptionResult.class)
                        .map(pr -> pr.getExceptionType() + ": " + pr.getMessage())
                        .orElse("unknown parse error"))
                .findFirst();
    }

    private static boolean parsesCleanly(final Path file, final String source) {
        final var ctx = new InMemoryExecutionContext();
        ctx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parseInputs(List.of(Parser.Input.fromString(file, source)), null, ctx)
                .toList();
        return firstParseError(parsed).isEmpty();
    }

    private static JsonObject property(final String type, final String description) {
        final JsonObject object = new JsonObject();
        object.addProperty("type", type);
        object.addProperty("description", description);
        return object;
    }

    private static String requireString(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required argument: " + key);
        }
        return value.toString();
    }

    private static int requireInt(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required argument: " + key);
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer, got: " + value);
        }
    }
}
