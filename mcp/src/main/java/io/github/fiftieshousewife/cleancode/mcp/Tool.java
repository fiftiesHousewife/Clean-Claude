package io.github.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * One MCP tool. Exposes its name, an input JSON schema the client can
 * validate against, a short description, and a {@link #call} entry point
 * that maps structured arguments to a {@link ToolResult}.
 *
 * <p>Implementations are pure with respect to process state (they may
 * touch the filesystem, but they do not talk to stdio) so they can be
 * unit-tested directly without spinning up the server.
 */
public interface Tool {

    String name();

    String description();

    JsonObject inputSchema();

    ToolResult call(Map<String, Object> arguments);
}
