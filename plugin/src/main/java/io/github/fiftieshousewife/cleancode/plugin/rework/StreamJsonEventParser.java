package io.github.fiftieshousewife.cleancode.plugin.rework;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Optional;

/**
 * Classifies one line emitted by {@code claude -p --output-format stream-json}
 * into a {@link StreamEvent}. Pure; the reader thread calls this per line and
 * routes the result either to the progress callback (for human-readable
 * events) or to the final-result capture (for the terminal {@code result}
 * event).
 */
public final class StreamJsonEventParser {

    private static final String TYPE = "type";
    private static final String ASSISTANT = "assistant";
    private static final String USER = "user";
    private static final String RESULT = "result";
    private static final String MESSAGE = "message";
    private static final String CONTENT = "content";
    private static final String TEXT = "text";
    private static final String TOOL_USE = "tool_use";
    private static final String TOOL_RESULT = "tool_result";
    private static final String USAGE = "usage";
    private static final String TOTAL_COST_USD = "total_cost_usd";
    private static final String DURATION_MS = "duration_ms";
    private static final String NUM_TURNS = "num_turns";
    private static final String NAME = "name";
    private static final String INPUT = "input";
    private static final int INPUT_SUMMARY_LIMIT = 100;

    private StreamJsonEventParser() {}

    public static StreamEvent parseLine(final String line) {
        if (line == null || line.isBlank()) {
            return new StreamEvent.Ignored();
        }
        try {
            final JsonElement parsed = JsonParser.parseString(line);
            if (!parsed.isJsonObject()) {
                return new StreamEvent.Ignored();
            }
            final JsonObject root = parsed.getAsJsonObject();
            final String type = stringOf(root, TYPE);
            return switch (type) {
                case ASSISTANT -> parseAssistantEvent(root);
                case USER -> parseUserEvent(root);
                case RESULT -> parseResultEvent(root);
                default -> new StreamEvent.Ignored();
            };
        } catch (JsonParseException e) {
            return new StreamEvent.Ignored();
        }
    }

    private static StreamEvent parseAssistantEvent(final JsonObject root) {
        final JsonArray content = contentArray(root);
        for (final JsonElement element : content) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject block = element.getAsJsonObject();
            final String blockType = stringOf(block, TYPE);
            if (TEXT.equals(blockType) && block.has(TEXT)) {
                return new StreamEvent.AssistantText(stringOf(block, TEXT));
            }
            if (TOOL_USE.equals(blockType) && block.has(NAME)) {
                return new StreamEvent.ToolUse(
                        stringOf(block, NAME),
                        summariseInput(stringOf(block, NAME), block.has(INPUT)
                                ? block.get(INPUT) : null));
            }
        }
        return new StreamEvent.Ignored();
    }

    private static StreamEvent parseUserEvent(final JsonObject root) {
        final JsonArray content = contentArray(root);
        for (final JsonElement element : content) {
            if (element.isJsonObject() && TOOL_RESULT.equals(stringOf(element.getAsJsonObject(), TYPE))) {
                return new StreamEvent.ToolResult();
            }
        }
        return new StreamEvent.Ignored();
    }

    private static StreamEvent parseResultEvent(final JsonObject root) {
        final String text = stringOf(root, RESULT);
        return new StreamEvent.Result(text, extractUsage(root));
    }

    private static JsonArray contentArray(final JsonObject root) {
        if (!root.has(MESSAGE) || !root.get(MESSAGE).isJsonObject()) {
            return new JsonArray();
        }
        final JsonObject message = root.getAsJsonObject(MESSAGE);
        if (!message.has(CONTENT) || !message.get(CONTENT).isJsonArray()) {
            return new JsonArray();
        }
        return message.getAsJsonArray(CONTENT);
    }

    private static Optional<AgentUsage> extractUsage(final JsonObject root) {
        if (!root.has(USAGE) || !root.get(USAGE).isJsonObject()) {
            return Optional.empty();
        }
        final JsonObject usage = root.getAsJsonObject(USAGE);
        return Optional.of(new AgentUsage(
                intOrZero(usage, "input_tokens"),
                intOrZero(usage, "output_tokens"),
                intOrZero(usage, "cache_creation_input_tokens"),
                intOrZero(usage, "cache_read_input_tokens"),
                longOrZero(root, DURATION_MS),
                intOrZero(root, NUM_TURNS),
                doubleOrZero(root, TOTAL_COST_USD)));
    }

    private static long longOrZero(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsLong() : 0L;
    }

    private static double doubleOrZero(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsDouble() : 0.0;
    }

    private static String summariseInput(final String toolName, final JsonElement input) {
        if (input == null || !input.isJsonObject()) {
            return "";
        }
        final JsonObject object = input.getAsJsonObject();
        final String primary = primaryFieldFor(toolName, object);
        return primary.length() <= INPUT_SUMMARY_LIMIT
                ? primary
                : primary.substring(0, INPUT_SUMMARY_LIMIT) + "…";
    }

    private static String primaryFieldFor(final String toolName, final JsonObject input) {
        for (final String key : fieldsWorthShowing(toolName)) {
            if (input.has(key) && input.get(key).isJsonPrimitive()) {
                return input.get(key).getAsString().replace('\n', ' ').strip();
            }
        }
        return input.keySet().stream().findFirst().map(k -> k + "=…").orElse("");
    }

    private static java.util.List<String> fieldsWorthShowing(final String toolName) {
        return switch (toolName) {
            case "Bash" -> java.util.List.of("command");
            case "Read", "Edit", "Write" -> java.util.List.of("file_path");
            case "Grep", "Glob" -> java.util.List.of("pattern");
            default -> java.util.List.of("file", "module", "pattern", "command", "file_path");
        };
    }

    private static String stringOf(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static int intOrZero(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsInt() : 0;
    }
}
