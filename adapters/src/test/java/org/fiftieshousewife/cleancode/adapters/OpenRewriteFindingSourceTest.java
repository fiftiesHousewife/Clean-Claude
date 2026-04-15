package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OpenRewriteFindingSourceTest {

    private OpenRewriteFindingSource source;

    @BeforeEach
    void setUp() {
        source = new OpenRewriteFindingSource();
    }

    @Test
    void id_returnsOpenrewrite() {
        assertEquals("openrewrite", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("OpenRewrite", source.displayName());
    }

    @Test
    void coveredCodes_containsF3() {
        assertTrue(source.coveredCodes().contains(HeuristicCode.F3));
    }

    @Test
    void collectFindings_producesF3ForFlagArguments(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Foo.java"), """
                package com.example;
                public class Foo {
                    public void doStuff(String name, boolean verbose) {}
                    private void hidden(boolean flag) {}
                }
                """);

        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        List<Finding> findings = source.collectFindings(ctx);

        assertEquals(1, findings.size());
        assertEquals(HeuristicCode.F3, findings.getFirst().code());
        assertEquals("openrewrite", findings.getFirst().tool());
        assertTrue(findings.getFirst().message().contains("verbose"));
    }

    @Test
    void collectFindings_emptySourceSet(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        List<Finding> findings = source.collectFindings(ctx);
        assertTrue(findings.isEmpty());
    }
}
