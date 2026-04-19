package io.github.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the set of {@link Tool}s the server exposes and answers the
 * MCP {@code tools/list} + {@code tools/call} requests by name.
 * Separate from {@link McpServer} so the registry can be driven directly
 * from tests without touching stdio.
 */
public final class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry register(final Tool tool) {
        tools.put(tool.name(), tool);
        return this;
    }

    public JsonObject list() {
        final JsonArray array = new JsonArray();
        tools.values().forEach(tool -> array.add(describe(tool)));
        final JsonObject result = new JsonObject();
        result.add("tools", array);
        return result;
    }

    public ToolResult call(final String name, final Map<String, Object> arguments) {
        final Tool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.error("unknown tool: " + name + " — known: " + String.join(", ", tools.keySet()));
        }
        return tool.call(arguments);
    }

    public List<String> toolNames() {
        return List.copyOf(tools.keySet());
    }

    private static JsonObject describe(final Tool tool) {
        final JsonObject description = new JsonObject();
        description.addProperty("name", tool.name());
        description.addProperty("description", tool.description());
        description.add("inputSchema", tool.inputSchema());
        return description;
    }
}
