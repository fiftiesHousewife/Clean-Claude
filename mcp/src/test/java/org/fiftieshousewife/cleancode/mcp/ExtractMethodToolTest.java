package org.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractMethodToolTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsCleanlyAndRewritesFileOnDisk() throws IOException {
        final Path file = tempDir.resolve("Greeter.java");
        Files.writeString(file, """
                package com.example;
                public class Greeter {
                    public void greet(String name) {
                        if (name == null) {
                            return;
                        }
                        System.out.println(name);
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 4,
                "endLine", 6,
                "newMethodName", "isMissing"));

        assertAll(
                () -> assertFalse(result.isError(), "happy path reports success"),
                () -> assertTrue(result.text().contains("extracted isMissing"),
                        "result names the helper"),
                () -> assertTrue(Files.readString(file).contains("boolean isMissing"),
                        "the recipe's output landed on disk"));
    }

    @Test
    void surfacesRecipeRejectionAsErrorWithReason() throws IOException {
        final Path file = tempDir.resolve("HasReturn.java");
        Files.writeString(file, """
                package com.example;
                public class HasReturn {
                    public int find(int[] xs, int target) {
                        for (int x : xs) {
                            if (x == target) {
                                return x;
                            }
                        }
                        return -1;
                    }
                }
                """);
        final String before = Files.readString(file);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 4,
                "endLine", 8,
                "newMethodName", "scan"));

        assertAll(
                () -> assertTrue(result.isError(),
                        "a recipe rejection surfaces as an error result"),
                () -> assertTrue(result.text().contains("break/continue/non-bare return")
                                || result.text().contains("reason"),
                        "the error text names the kind of rejection"),
                () -> assertTrue(Files.readString(file).equals(before),
                        "rejected extractions leave the file byte-identical"));
    }

    @Test
    void reportsBadArgumentsWhenLineNumberIsNotNumeric() throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, "class Foo {}");

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", "not-a-number",
                "endLine", 5,
                "newMethodName", "x"));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("bad arguments")));
    }

    @Test
    void schemaDeclaresAllFourRequiredProperties() {
        final var schema = new ExtractMethodTool().inputSchema();
        final var required = schema.getAsJsonArray("required");
        assertAll(
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("file"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("startLine"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("endLine"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("newMethodName"))));
    }
}
