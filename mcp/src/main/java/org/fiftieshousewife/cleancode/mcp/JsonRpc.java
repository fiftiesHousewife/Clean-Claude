package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builders and accessors for the subset of JSON-RPC 2.0 the MCP server
 * exchanges. Kept free of transport concerns so the stdio loop,
 * tool-dispatch code, and tests can share one vocabulary.
 */
public final class JsonRpc {

    public static final String VERSION = "2.0";
    public static final String JSONRPC = "jsonrpc";
    public static final String ID = "id";
    public static final String METHOD = "method";
    public static final String PARAMS = "params";
    public static final String RESULT = "result";
    public static final String ERROR = "error";

    private JsonRpc() {}

    public static JsonObject parse(final String line) {
        return JsonParser.parseString(line).getAsJsonObject();
    }

    public static JsonObject success(final JsonElement id, final JsonElement result) {
        final JsonObject response = envelope(id);
        response.add(RESULT, result);
        return response;
    }

    public static JsonObject error(final JsonElement id, final int code, final String message) {
        final JsonObject response = envelope(id);
        final JsonObject err = new JsonObject();
        err.addProperty("code", code);
        err.addProperty("message", message);
        response.add(ERROR, err);
        return response;
    }

    public static JsonObject toolCallResult(final ToolResult result) {
        final JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", result.text());
        final JsonArray contentArray = new JsonArray();
        contentArray.add(content);
        final JsonObject object = new JsonObject();
        object.add("content", contentArray);
        object.addProperty("isError", result.isError());
        return object;
    }

    public static Map<String, Object> argumentsOf(final JsonObject params) {
        if (!params.has("arguments") || !params.get("arguments").isJsonObject()) {
            return Map.of();
        }
        return toMap(params.getAsJsonObject("arguments"));
    }

    private static Map<String, Object> toMap(final JsonObject object) {
        final Map<String, Object> map = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> map.put(entry.getKey(), unwrap(entry.getValue())));
        return map;
    }

    private static Object unwrap(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            final var primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsNumber();
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            return primitive.getAsString();
        }
        if (element.isJsonObject()) {
            return toMap(element.getAsJsonObject());
        }
        return element.toString();
    }

    private static JsonObject envelope(final JsonElement id) {
        final JsonObject envelope = new JsonObject();
        envelope.addProperty(JSONRPC, VERSION);
        if (id != null) {
            envelope.add(ID, id);
        }
        return envelope;
    }
}
