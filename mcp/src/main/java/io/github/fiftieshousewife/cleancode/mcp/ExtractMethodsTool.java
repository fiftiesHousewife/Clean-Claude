package io.github.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.fiftieshousewife.cleancode.refactoring.extractmethod.ExtractMethodRecipe;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Batch counterpart to {@link ExtractMethodTool}. Takes a single file and
 * an array of ranges. Applies each extraction against the in-flight source
 * in bottom-up order (highest startLine first) so earlier ranges still
 * map to their original line numbers after later helpers are appended at
 * the end of the class.
 *
 * <p>Transactional: if any range rejects or fails to re-parse, the file
 * on disk is left untouched and the result surfaces per-range status.
 * Collapses what was N tool-call turns for N extractions into one.
 */
public final class ExtractMethodsTool implements Tool {

    private static final String NAME = "extract_methods";
    private static final String FILE = "file";
    private static final String RANGES = "ranges";
    private static final String START_LINE = "startLine";
    private static final String END_LINE = "endLine";
    private static final String NEW_METHOD_NAME = "newMethodName";
    private static final String REQUIRE_PRINT_EQUALS_INPUT = "org.openrewrite.requirePrintEqualsInput";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Extract multiple ranges from the same Java file in one call. Takes a `ranges` "
                + "array; each range is {startLine, endLine, newMethodName}. Ranges are "
                + "applied bottom-up internally so the startLine/endLine you pass always "
                + "refer to the original file's line numbers. Transactional: if any range "
                + "rejects, no change is written. Prefer this over repeated extract_method "
                + "calls when you have 2+ extractions planned on the same file.";
    }

    @Override
    public JsonObject inputSchema() {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        final JsonObject properties = new JsonObject();
        properties.add(FILE, stringProp("Path to the target Java file."));
        properties.add(RANGES, rangesArraySchema());
        schema.add("properties", properties);
        final JsonArray required = new JsonArray();
        required.add(FILE);
        required.add(RANGES);
        schema.add("required", required);
        return schema;
    }

    private static JsonObject rangesArraySchema() {
        final JsonObject array = new JsonObject();
        array.addProperty("type", "array");
        array.addProperty("description",
                "One or more extractions to apply; each has startLine (1-based inclusive), "
                        + "endLine (1-based inclusive), and newMethodName (Java identifier).");
        final JsonObject item = new JsonObject();
        item.addProperty("type", "object");
        final JsonObject itemProps = new JsonObject();
        itemProps.add(START_LINE, intProp("1-based inclusive range start."));
        itemProps.add(END_LINE, intProp("1-based inclusive range end."));
        itemProps.add(NEW_METHOD_NAME, stringProp("Name for the extracted helper."));
        item.add("properties", itemProps);
        final JsonArray itemRequired = new JsonArray();
        itemRequired.add(START_LINE);
        itemRequired.add(END_LINE);
        itemRequired.add(NEW_METHOD_NAME);
        item.add("required", itemRequired);
        array.add("items", item);
        return array;
    }

    @Override
    public ToolResult call(final Map<String, Object> arguments) {
        try {
            final Path file = Path.of(requireString(arguments, FILE));
            final List<RangeRequest> ranges = parseRanges(arguments.get(RANGES));
            if (ranges.isEmpty()) {
                return ToolResult.error("bad arguments: ranges must be a non-empty array");
            }
            return runBatch(file, ranges);
        } catch (IllegalArgumentException e) {
            return ToolResult.error("bad arguments: " + e.getMessage());
        } catch (IOException e) {
            return ToolResult.error("io error: " + e.getMessage());
        }
    }

    private static ToolResult runBatch(final Path file, final List<RangeRequest> ranges)
            throws IOException {
        final String original = Files.readString(file);
        final int originalLines = ExtractMethodTool.countLines(original);
        final List<RangeRequest> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(RangeRequest::startLine).reversed());
        final Optional<String> overlap = firstOverlap(sorted);
        if (overlap.isPresent()) {
            return ToolResult.error("bad arguments: " + overlap.get());
        }
        String current = original;
        final List<String> extracted = new ArrayList<>();
        for (final RangeRequest range : sorted) {
            final Optional<String> next = applyOne(file, current, range);
            if (next.isEmpty()) {
                return ToolResult.error("extract_methods aborted on range "
                        + range.startLine() + "-" + range.endLine()
                        + " (" + range.newMethodName() + "): nothing written. "
                        + "Preceding ranges in this batch were rolled back.");
            }
            current = next.get();
            extracted.add(range.newMethodName());
        }
        Files.writeString(file, current);
        return ToolResult.ok("extracted " + extracted.size() + " methods: "
                + String.join(", ", extracted)
                + "; file " + originalLines + " -> " + ExtractMethodTool.countLines(current) + " lines");
    }

    private static Optional<String> applyOne(final Path file, final String source,
                                             final RangeRequest range) {
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        final Parser.Input input = Parser.Input.fromString(file, source);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .build()
                .parseInputs(List.of(input), null, ctx)
                .toList();
        if (parsed.isEmpty() || !(parsed.getFirst() instanceof J.CompilationUnit cu)) {
            return Optional.empty();
        }
        final ExtractMethodRecipe recipe = new ExtractMethodRecipe(
                file.getFileName().toString(), range.startLine(), range.endLine(), range.newMethodName());
        return recipe.extractTextually(cu, source, new Cursor(null, cu));
    }

    private static Optional<String> firstOverlap(final List<RangeRequest> sortedDescending) {
        for (int i = 0; i < sortedDescending.size() - 1; i++) {
            final RangeRequest higher = sortedDescending.get(i);
            final RangeRequest lower = sortedDescending.get(i + 1);
            if (lower.endLine() >= higher.startLine()) {
                return Optional.of("ranges overlap: "
                        + lower.startLine() + "-" + lower.endLine() + " and "
                        + higher.startLine() + "-" + higher.endLine());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static List<RangeRequest> parseRanges(final Object raw) {
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("ranges must be an array");
        }
        final List<RangeRequest> out = new ArrayList<>();
        for (final Object element : list) {
            if (!(element instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("each range must be an object");
            }
            out.add(new RangeRequest(
                    requireInt((Map<String, Object>) map, START_LINE),
                    requireInt((Map<String, Object>) map, END_LINE),
                    requireString((Map<String, Object>) map, NEW_METHOD_NAME)));
        }
        return out;
    }

    private record RangeRequest(int startLine, int endLine, String newMethodName) {}

    private static JsonObject stringProp(final String description) {
        final JsonObject object = new JsonObject();
        object.addProperty("type", "string");
        object.addProperty("description", description);
        return object;
    }

    private static JsonObject intProp(final String description) {
        final JsonObject object = new JsonObject();
        object.addProperty("type", "integer");
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
