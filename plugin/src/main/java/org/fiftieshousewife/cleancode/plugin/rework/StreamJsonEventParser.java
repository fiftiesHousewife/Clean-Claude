package org.fiftieshousewife.cleancode.plugin.rework;

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
    private static final String NAME = "name";

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
                return new StreamEvent.ToolUse(stringOf(block, NAME));
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
        final double costUsd = root.has(TOTAL_COST_USD) && root.get(TOTAL_COST_USD).isJsonPrimitive()
                ? root.get(TOTAL_COST_USD).getAsDouble()
                : 0.0;
        return Optional.of(new AgentUsage(
                intOrZero(usage, "input_tokens"),
                intOrZero(usage, "output_tokens"),
                intOrZero(usage, "cache_creation_input_tokens"),
                intOrZero(usage, "cache_read_input_tokens"),
                costUsd));
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
