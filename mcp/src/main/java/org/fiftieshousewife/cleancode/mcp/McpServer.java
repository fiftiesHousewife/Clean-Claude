package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/**
 * MCP server entry point. Reads one JSON-RPC message per line from
 * stdin, dispatches it to {@link ToolRegistry}, writes the response
 * to stdout (one message per line). Supports the subset of the MCP
 * protocol the Claude Code client uses: {@code initialize},
 * {@code notifications/initialized}, {@code tools/list}, and
 * {@code tools/call}. Unknown methods return a JSON-RPC
 * "method not found" error rather than crashing the connection.
 *
 * <p>Factored so {@link #handle} is a pure string→string function —
 * tests wire that directly and skip stdio, while {@link #main} wires
 * real stdio at process entry.
 */
public final class McpServer {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "cleancode-refactoring";
    private static final String SERVER_VERSION = "1.0.0";
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;

    private final ToolRegistry registry;

    public McpServer(final ToolRegistry registry) {
        this.registry = registry;
    }

    public static void main(final String[] args) throws IOException {
        final GradleInvoker gradle = new GradleInvoker(Path.of(System.getProperty("user.dir")));
        final McpServer server = new McpServer(defaultRegistry(gradle));
        try (Reader in = new InputStreamReader(System.in, StandardCharsets.UTF_8);
             Writer out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            server.run(new BufferedReader(in), new BufferedWriter(out));
        }
    }

    /**
     * The set of tools the production server ships. Public so tests and
     * permission audits can enumerate them without spawning a process.
     */
    public static ToolRegistry defaultRegistry(final GradleInvoker gradle) {
        return new ToolRegistry()
                .register(new ExtractMethodTool())
                .register(new VerifyBuildTool(gradle))
                .register(new RunTestsTool(gradle))
                .register(new FormatTool(gradle));
    }

    public static String serverName() {
        return SERVER_NAME;
    }

    public void run(final BufferedReader in, final BufferedWriter out) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            final Optional<String> response = handle(line);
            if (response.isPresent()) {
                out.write(response.get());
                out.write('\n');
                out.flush();
            }
        }
    }

    public Optional<String> handle(final String messageJson) {
        final JsonObject message;
        try {
            message = JsonRpc.parse(messageJson);
        } catch (JsonParseException e) {
            return Optional.of(JsonRpc.error(JsonNull.INSTANCE, INTERNAL_ERROR,
                    "failed to parse JSON-RPC message: " + e.getMessage()).toString());
        }
        final String method = message.has(JsonRpc.METHOD) ? message.get(JsonRpc.METHOD).getAsString() : "";
        final JsonElement id = message.get(JsonRpc.ID);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(dispatch(method, id, message).toString());
    }

    private JsonObject dispatch(final String method, final JsonElement id, final JsonObject message) {
        return switch (method) {
            case "initialize" -> JsonRpc.success(id, initializeResult());
            case "tools/list" -> JsonRpc.success(id, registry.list());
            case "tools/call" -> callTool(id, message);
            default -> JsonRpc.error(id, METHOD_NOT_FOUND, "method not found: " + method);
        };
    }

    private JsonObject callTool(final JsonElement id, final JsonObject message) {
        final JsonObject params = message.has(JsonRpc.PARAMS) && message.get(JsonRpc.PARAMS).isJsonObject()
                ? message.getAsJsonObject(JsonRpc.PARAMS)
                : new JsonObject();
        if (!params.has("name") || !params.get("name").isJsonPrimitive()) {
            return JsonRpc.error(id, INVALID_PARAMS, "tools/call is missing `name`");
        }
        final String name = params.get("name").getAsString();
        final ToolResult result = registry.call(name, JsonRpc.argumentsOf(params));
        return JsonRpc.success(id, JsonRpc.toolCallResult(result));
    }

    private static JsonObject initializeResult() {
        final JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", SERVER_NAME);
        serverInfo.addProperty("version", SERVER_VERSION);
        final JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        final JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);
        result.add("capabilities", capabilities);
        result.add("serverInfo", serverInfo);
        return result;
    }
}
