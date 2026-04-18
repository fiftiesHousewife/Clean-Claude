package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonObject;
import org.fiftieshousewife.cleancode.refactoring.extractmethod.ExtractMethodRecipe;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    private static ToolResult runExtract(final Path file, final int startLine, final int endLine,
                                         final String newMethodName) throws IOException {
        final String source = Files.readString(file);
        final ExtractMethodRecipe recipe = new ExtractMethodRecipe(
                file.getFileName().toString(), startLine, endLine, newMethodName);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final List<Result> changed = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset().getAllResults();
        if (changed.isEmpty()) {
            return ToolResult.error("recipe rejected the range — common reasons: contains "
                    + "break/continue/non-bare return, more than one output variable, or the "
                    + "range does not align with top-level statement boundaries. Inspect the "
                    + "source and try a narrower range.");
        }
        Files.writeString(file, changed.getFirst().getAfter().printAll());
        return ToolResult.ok("extracted " + newMethodName + " from lines "
                + startLine + "-" + endLine + " of " + file);
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
