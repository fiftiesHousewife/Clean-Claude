package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;
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
    void severityFor_escalatesLatentBugCodesToError() {
        assertAll(
                () -> assertEquals(Severity.ERROR,
                        OpenRewriteFindingSource.severityFor(HeuristicCode.G4)),
                () -> assertEquals(Severity.ERROR,
                        OpenRewriteFindingSource.severityFor(HeuristicCode.Ch7_1)),
                () -> assertEquals(Severity.ERROR,
                        OpenRewriteFindingSource.severityFor(HeuristicCode.F2)),
                () -> assertEquals(Severity.ERROR,
                        OpenRewriteFindingSource.severityFor(HeuristicCode.G8)));
    }

    @Test
    void severityFor_defaultsToWarningForCodesWithoutAnEntry() {
        assertEquals(Severity.WARNING,
                OpenRewriteFindingSource.severityFor(HeuristicCode.G30));
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
    void collectFindings_reportsCorrectLineForMethodAfterAnotherMethod(@TempDir Path tempDir) throws Exception {
        // Reproduces a widespread line-number bug seen in real reports: every
        // method after the first lands on a wrong line because buildLineIndex
        // misses whitespace inside the previous method's J.Block.end (the
        // closing-brace region). The Ch7_1 finding for catchOnlyLogs MUST
        // report the line of the catchOnlyLogs declaration itself, not some
        // earlier line that drifts further off with each preceding method.
        final Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceDir);
        // Build the fixture so the *catchOnlyLogs* method declaration lands on
        // a known line (12). The recipe reports method-level findings, and
        // findingForMethod resolves to the method declaration line.
        final String src = """
                package com.example;
                public class Foo {
                    public void first() {
                        System.out.println("hi");
                    }

                    public void second() {
                        System.out.println("hi");
                    }

                    public void catchOnlyLogs() {
                        try {
                            doSomething();
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }

                    private void doSomething() throws Exception {}
                }
                """;
        Files.writeString(sourceDir.resolve("Foo.java"), src);

        final ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        final List<Finding> findings = source.collectFindings(ctx);
        final Finding catchFinding = findings.stream()
                .filter(f -> f.code() == HeuristicCode.Ch7_1)
                .filter(f -> f.message().contains("catchOnlyLogs"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No Ch7_1 finding for catchOnlyLogs in: " + findings));

        // catchOnlyLogs is declared on line 11 of the source above (1-indexed,
        // counting the 'package' line as 1). If buildLineIndex is correct,
        // startLine resolves to 11. The bug reports it lower — typically a
        // line inside `second()` or even on `first()` because the indexer
        // never advances past previous methods' closing braces.
        assertEquals(11, catchFinding.startLine(),
                "Ch7_1 finding should point at the declaration of catchOnlyLogs (line 11), "
                        + "not drift back into preceding methods. Got: " + catchFinding.startLine());
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
