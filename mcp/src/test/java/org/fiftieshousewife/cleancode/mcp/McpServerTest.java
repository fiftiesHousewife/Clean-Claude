package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerTest {

    @TempDir
    Path tempDir;

    private final McpServer server = new McpServer(
            new ToolRegistry().register(new ExtractMethodTool()));

    @Test
    void initializeReturnsProtocolVersionAndToolsCapability() {
        final String request = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""";

        final JsonObject response = invoke(request);

        final JsonObject result = response.getAsJsonObject("result");
        assertAll(
                () -> assertEquals("2.0", response.get("jsonrpc").getAsString()),
                () -> assertEquals(1, response.get("id").getAsInt()),
                () -> assertEquals("2024-11-05", result.get("protocolVersion").getAsString()),
                () -> assertTrue(result.getAsJsonObject("capabilities").has("tools"),
                        "tools capability is advertised"),
                () -> assertEquals("cleancode-refactoring",
                        result.getAsJsonObject("serverInfo").get("name").getAsString()));
    }

    @Test
    void toolsListReturnsExtractMethodWithSchema() {
        final String request = """
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}""";

        final JsonObject response = invoke(request);
        final var tools = response.getAsJsonObject("result").getAsJsonArray("tools");

        assertAll(
                () -> assertEquals(1, tools.size()),
                () -> assertEquals("extract_method",
                        tools.get(0).getAsJsonObject().get("name").getAsString()),
                () -> assertTrue(tools.get(0).getAsJsonObject().has("inputSchema"),
                        "schema travels with the tool advertisement"));
    }

    @Test
    void toolsCallHappyPathReturnsIsErrorFalseAndWritesFile() throws IOException {
        final Path file = tempDir.resolve("Guarded.java");
        Files.writeString(file, """
                package com.example;
                public class Guarded {
                    public void process(String input) {
                        if (input == null) {
                            return;
                        }
                        System.out.println(input);
                    }
                }
                """);
        final String request = """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{
                    "name":"extract_method",
                    "arguments":{"file":"%s","startLine":4,"endLine":6,"newMethodName":"isMissing"}
                }}""".formatted(file.toString().replace("\\", "\\\\"));

        final JsonObject response = invoke(request);
        final JsonObject result = response.getAsJsonObject("result");

        assertAll(
                () -> assertFalse(result.get("isError").getAsBoolean()),
                () -> assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject()
                                .get("text").getAsString().contains("extracted isMissing"),
                        "content block names the helper"),
                () -> assertTrue(Files.readString(file).contains("boolean isMissing"),
                        "the recipe's output landed on disk"));
    }

    @Test
    void unknownMethodReturnsMethodNotFoundError() {
        final String request = """
                {"jsonrpc":"2.0","id":9,"method":"does/not/exist"}""";

        final JsonObject response = invoke(request);
        final JsonObject error = response.getAsJsonObject("error");

        assertAll(
                () -> assertEquals(-32601, error.get("code").getAsInt()),
                () -> assertTrue(error.get("message").getAsString().contains("does/not/exist")));
    }

    @Test
    void notificationsInitializedHasNoIdAndYieldsNoResponse() {
        final String request = """
                {"jsonrpc":"2.0","method":"notifications/initialized"}""";

        final Optional<String> response = server.handle(request);

        assertTrue(response.isEmpty(),
                "MCP notifications — messages without an id — must not elicit a response");
    }

    private JsonObject invoke(final String request) {
        return JsonRpc.parse(server.handle(request).orElseThrow());
    }
}
