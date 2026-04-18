package org.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractMethodsToolTest {

    @TempDir
    Path tempDir;

    @Test
    void batchExtractsMultipleRangesAndWritesFileOnce() throws IOException {
        final Path file = threePhaseFixture();

        final ToolResult result = new ExtractMethodsTool().call(Map.of(
                "file", file.toString(),
                "ranges", List.of(
                        Map.of("startLine", 4, "endLine", 6, "newMethodName", "phaseOne"),
                        Map.of("startLine", 7, "endLine", 9, "newMethodName", "phaseTwo"),
                        Map.of("startLine", 10, "endLine", 12, "newMethodName", "phaseThree"))));

        final String after = Files.readString(file);
        assertAll(
                () -> assertFalse(result.isError(), "batch should succeed: " + result.text()),
                () -> assertTrue(result.text().contains("extracted 3 methods"),
                        "response names the count: " + result.text()),
                () -> assertTrue(after.contains("phaseOne(")),
                () -> assertTrue(after.contains("phaseTwo(")),
                () -> assertTrue(after.contains("phaseThree(")));
    }

    @Test
    void rangesInAnyOrderAreAppliedBottomUp() throws IOException {
        final Path file = threePhaseFixture();

        final ToolResult result = new ExtractMethodsTool().call(Map.of(
                "file", file.toString(),
                "ranges", List.of(
                        Map.of("startLine", 4, "endLine", 6, "newMethodName", "phaseOne"),
                        Map.of("startLine", 10, "endLine", 12, "newMethodName", "phaseThree"),
                        Map.of("startLine", 7, "endLine", 9, "newMethodName", "phaseTwo"))));

        assertFalse(result.isError(), "sort-descending internally: " + result.text());
        final String after = Files.readString(file);
        assertAll(
                () -> assertTrue(after.contains("phaseOne(")),
                () -> assertTrue(after.contains("phaseTwo(")),
                () -> assertTrue(after.contains("phaseThree(")));
    }

    @Test
    void overlappingRangesAreRejectedBeforeAnyWrite() throws IOException {
        final Path file = threePhaseFixture();
        final String before = Files.readString(file);

        final ToolResult result = new ExtractMethodsTool().call(Map.of(
                "file", file.toString(),
                "ranges", List.of(
                        Map.of("startLine", 4, "endLine", 8, "newMethodName", "first"),
                        Map.of("startLine", 7, "endLine", 10, "newMethodName", "second"))));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("overlap"),
                        "error names the problem: " + result.text()),
                () -> assertEquals(before, Files.readString(file),
                        "rejection leaves the file byte-identical"));
    }

    @Test
    void anyRangeFailingRollsBackEntireBatch() throws IOException {
        final Path file = threePhaseFixture();
        final String before = Files.readString(file);

        final ToolResult result = new ExtractMethodsTool().call(Map.of(
                "file", file.toString(),
                "ranges", List.of(
                        Map.of("startLine", 4, "endLine", 6, "newMethodName", "phaseOne"),
                        Map.of("startLine", 999, "endLine", 1000, "newMethodName", "outOfRange"))));

        assertAll(
                () -> assertTrue(result.isError(), "out-of-range line aborts the batch"),
                () -> assertTrue(result.text().contains("aborted")),
                () -> assertEquals(before, Files.readString(file),
                        "earlier successful ranges in the batch are rolled back"));
    }

    @Test
    void emptyRangesListIsBadArguments() {
        final ToolResult result = new ExtractMethodsTool().call(Map.of(
                "file", "/tmp/doesnt-matter.java",
                "ranges", List.of()));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("non-empty")));
    }

    @Test
    void schemaDeclaresFileAndRangesRequired() {
        final var schema = new ExtractMethodsTool().inputSchema();
        final var required = schema.getAsJsonArray("required");
        assertAll(
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("file"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("ranges"))));
    }

    private Path threePhaseFixture() throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    public void run() {
                        System.out.println("a");
                        System.out.println("b");
                        System.out.println("c");
                        System.out.println("d");
                        System.out.println("e");
                        System.out.println("f");
                        System.out.println("g");
                        System.out.println("h");
                        System.out.println("i");
                    }
                }
                """);
        return file;
    }
}
