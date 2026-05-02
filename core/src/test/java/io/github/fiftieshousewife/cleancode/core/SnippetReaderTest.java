package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnippetReaderTest {

    @Test
    void returnsSurroundingContextForAFinding(@TempDir Path tempDir) throws Exception {
        final Path file = tempDir.resolve("src/main/java/Foo.java");
        Files.createDirectories(file.getParent());
        Files.write(file, List.of(
                "package com.example;",
                "",
                "public class Foo {",
                "    public void bar() {",
                "        System.out.println(\"oops\");",
                "    }",
                "}"));

        final Finding finding = new Finding(HeuristicCode.G17, "src/main/java/Foo.java",
                5, 5, "System.out is misplaced", Severity.WARNING, Confidence.HIGH,
                "openrewrite", "G17", java.util.Map.of());

        final Optional<SnippetReader.Snippet> snippet = SnippetReader.read(finding, tempDir);

        assertTrue(snippet.isPresent());
        assertAll(
                () -> assertEquals(3, snippet.get().firstLineNumber(),
                        "two lines of context before the focal line"),
                () -> assertEquals(5, snippet.get().focalStartLine()),
                () -> assertEquals(5, snippet.get().focalEndLine()),
                () -> assertEquals(5, snippet.get().lines().size(),
                        "lines 3..7 inclusive"),
                () -> assertTrue(snippet.get().lines().contains("        System.out.println(\"oops\");")));
    }

    @Test
    void returnsEmptyForProjectLevelFindings(@TempDir Path tempDir) {
        final Finding projectFinding = Finding.projectLevel(HeuristicCode.T1, "Low coverage",
                Severity.ERROR, Confidence.HIGH, "jacoco", "coverage");

        assertFalse(SnippetReader.read(projectFinding, tempDir).isPresent(),
                "project-level findings have no source line");
    }

    @Test
    void returnsEmptyForMissingFile(@TempDir Path tempDir) {
        final Finding finding = new Finding(HeuristicCode.G5, "src/main/java/Missing.java",
                1, 1, "duplicated", Severity.WARNING, Confidence.HIGH,
                "cpd", "cpd-dup", java.util.Map.of());

        assertFalse(SnippetReader.read(finding, tempDir).isPresent(),
                "deleted/moved files should not blow up the report");
    }

    @Test
    void returnsEmptyWhenProjectRootIsNull(@TempDir Path tempDir) {
        final Finding finding = new Finding(HeuristicCode.G5, "Foo.java",
                1, 1, "x", Severity.WARNING, Confidence.HIGH,
                "cpd", "cpd-dup", java.util.Map.of());

        assertFalse(SnippetReader.read(finding, null).isPresent());
    }

    @Test
    void clampsLineRangeToFileBounds(@TempDir Path tempDir) throws Exception {
        final Path file = tempDir.resolve("Foo.java");
        Files.write(file, List.of("line1", "line2", "line3"));

        final Finding finding = new Finding(HeuristicCode.G5, "Foo.java",
                1, 100, "weird range", Severity.WARNING, Confidence.HIGH,
                "cpd", "cpd-dup", java.util.Map.of());

        final SnippetReader.Snippet snippet = SnippetReader.read(finding, tempDir).orElseThrow();
        assertAll(
                () -> assertEquals(1, snippet.firstLineNumber()),
                () -> assertEquals(3, snippet.lines().size(),
                        "should not crash when endLine exceeds file length"));
    }

    @Test
    void capsLargeWindowsAtMaxLines(@TempDir Path tempDir) throws Exception {
        final Path file = tempDir.resolve("Big.java");
        Files.write(file, IntStream.rangeClosed(1, 50)
                .mapToObj(i -> "line " + i)
                .toList());

        final Finding finding = new Finding(HeuristicCode.G5, "Big.java",
                10, 30, "big block", Severity.WARNING, Confidence.HIGH,
                "cpd", "cpd-dup", java.util.Map.of());

        final SnippetReader.Snippet snippet = SnippetReader.read(finding, tempDir).orElseThrow();
        assertEquals(SnippetReader.MAX_LINES, snippet.lines().size(),
                "snippet truncates to MAX_LINES so the report stays small");
    }
}
