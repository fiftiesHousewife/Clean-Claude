package io.github.fiftieshousewife.cleancode.plugin.rework;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses the stdout produced by {@code claude -p} when the brief asks
 * the agent to emit a structured summary of its rework pass. The agent
 * is instructed to emit raw JSON with {@code actions} and {@code rejected}
 * arrays, but real output often comes wrapped in chat prose or fenced
 * with ```json — this parser finds the first object that looks like our
 * schema and reads it, ignoring surrounding text.
 */
public final class AgentResponseParser {

    private static final String ACTIONS_KEY = "actions";
    private static final String REJECTED_KEY = "rejected";
    private static final String RECIPE_KEY = "recipe";
    private static final String OPTIONS_KEY = "options";
    private static final String WHY_KEY = "why";
    private static final Gson GSON = new Gson();

    private AgentResponseParser() {}

    public record Parsed(List<AgentAction> actions, List<AgentRejection> rejected,
                         Optional<String> error) {}

    public static Parsed parse(final String stdout) {
        final Optional<JsonObject> root = locateSchemaObject(stdout);
        if (root.isEmpty()) {
            return new Parsed(List.of(), List.of(),
                    Optional.of("agent output contained no JSON object with actions/rejected keys"));
        }
        final List<AgentAction> actions = readActions(root.get());
        final List<AgentRejection> rejected = readRejections(root.get());
        return new Parsed(actions, rejected, Optional.empty());
    }

    private static Optional<JsonObject> locateSchemaObject(final String stdout) {
        int from = 0;
        while (from < stdout.length()) {
            final int start = stdout.indexOf('{', from);
            if (start < 0) {
                return Optional.empty();
            }
            final int end = findMatchingBrace(stdout, start);
            if (end < 0) {
                return Optional.empty();
            }
            final String candidate = stdout.substring(start, end + 1);
            try {
                final JsonElement parsed = JsonParser.parseString(candidate);
                if (parsed.isJsonObject() && looksLikeSchema(parsed.getAsJsonObject())) {
                    return Optional.of(parsed.getAsJsonObject());
                }
            } catch (JsonParseException ignored) {
                // candidate wasn't valid JSON; keep scanning
            }
            from = start + 1;
        }
        return Optional.empty();
    }

    private static boolean looksLikeSchema(final JsonObject object) {
        return object.has(ACTIONS_KEY) || object.has(REJECTED_KEY);
    }

    private static int findMatchingBrace(final String text, final int openPos) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (inString) {
                if (c == '\\' && i + 1 < text.length()) {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static List<AgentAction> readActions(final JsonObject root) {
        final List<AgentAction> actions = new ArrayList<>();
        final JsonArray array = root.has(ACTIONS_KEY) ? root.getAsJsonArray(ACTIONS_KEY) : new JsonArray();
        array.forEach(el -> {
            if (el.isJsonObject()) {
                final JsonObject o = el.getAsJsonObject();
                actions.add(new AgentAction(stringOf(o, RECIPE_KEY), optionsOf(o), stringOf(o, WHY_KEY)));
            }
        });
        return actions;
    }

    private static List<AgentRejection> readRejections(final JsonObject root) {
        final List<AgentRejection> rejections = new ArrayList<>();
        final JsonArray array = root.has(REJECTED_KEY) ? root.getAsJsonArray(REJECTED_KEY) : new JsonArray();
        array.forEach(el -> {
            if (el.isJsonObject()) {
                final JsonObject o = el.getAsJsonObject();
                rejections.add(new AgentRejection(stringOf(o, RECIPE_KEY), optionsOf(o), stringOf(o, WHY_KEY)));
            }
        });
        return rejections;
    }

    private static String stringOf(final JsonObject o, final String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionsOf(final JsonObject o) {
        if (!o.has(OPTIONS_KEY) || !o.get(OPTIONS_KEY).isJsonObject()) {
            return new LinkedHashMap<>();
        }
        return GSON.fromJson(o.get(OPTIONS_KEY), LinkedHashMap.class);
    }
}
