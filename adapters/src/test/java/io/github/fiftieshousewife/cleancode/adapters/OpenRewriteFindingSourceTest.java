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
    void collectFindings_producesF3ForFlagArguments(@TempDir final Path tempDir) throws Exception {
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
    void collectFindings_reportsCorrectLineForMethodAfterAnotherMethod(@TempDir final Path tempDir) throws Exception {
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

        // The catch keyword for catchOnlyLogs sits on line 14 of the source
        // above (1-indexed, package line is 1). Ch7_1 now anchors at the
        // catch keyword (per the user's "highlight the actual issue, not
        // the surrounding method" feedback). Pre-fix this would have
        // landed on line 9 because the indexer never advanced past
        // first()'s closing brace; today it correctly resolves to 14.
        assertEquals(14, catchFinding.startLine(),
                "Ch7_1 finding should point at the catch keyword line (14), not drift earlier. "
                        + "Got: " + catchFinding.startLine());
    }

    @Test
    void collectFindings_pointsCh7_1AtCatchKeywordNotMethodDeclaration(@TempDir final Path tempDir) throws Exception {
        // The catch keyword may sit many lines below the method
        // declaration. The snippet should show the empty/log-only catch
        // body, not the method's signature/Javadoc.
        final Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Foo.java"), """
                package com.example;
                public class Foo {
                    /** Some Javadoc that should not be the highlighted line. */
                    public void doIt() {
                        try {
                            workItem();
                            workItem();
                            workItem();
                        } catch (java.io.IOException e) {
                            System.out.println(e);
                        }
                    }
                    private void workItem() throws java.io.IOException {}
                }
                """);

        final ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        final Finding catchFinding = source.collectFindings(ctx).stream()
                .filter(f -> f.code() == HeuristicCode.Ch7_1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a Ch7_1 finding"));

        // Line 9 in the fixture above contains `} catch (java.io.IOException e) {`.
        assertEquals(9, catchFinding.startLine(),
                "Ch7_1 should point at the catch keyword line, not the method declaration. Got: "
                        + catchFinding.startLine());
    }

    @Test
    void collectFindings_emptySourceSet(@TempDir final Path tempDir) throws Exception {
        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        List<Finding> findings = source.collectFindings(ctx);
        assertTrue(findings.isEmpty());
    }

    @Test
    void collectFindings_disambiguatesOverloadedMethodsByParamCount(@TempDir final Path tempDir) throws Exception {
        // Two public methods named `process` differ only in arity. F3
        // should fire for the FOUR-parameter overload (the one with the
        // boolean) and the finding line MUST land on the four-arg
        // declaration, not the first matching name.
        final Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceDir);
        final String src = """
                package com.example;
                public class Foo {
                    public void process(String name) {
                        // 1-arg overload — no flag, no F3 fires here.
                    }

                    public void process(String name,
                                        java.util.Map<String, Integer> labels,
                                        int retries,
                                        boolean dryRun) {
                        // 4-arg overload — F3 should land here.
                    }
                }
                """;
        Files.writeString(sourceDir.resolve("Foo.java"), src);

        final ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        final Finding f3 = source.collectFindings(ctx).stream()
                .filter(f -> f.code() == HeuristicCode.F3)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected an F3 finding"));

        // The 4-arg `process` declaration starts on line 7 of the fixture
        // (1-indexed package=line 1). Without param-count disambiguation
        // the finding would land on line 3 (the 1-arg overload), which
        // is wildly misleading because that overload doesn't even have
        // a boolean parameter.
        assertEquals(7, f3.startLine(),
                "F3 must anchor at the 4-arg overload (line 7), not the 1-arg one (line 3). Got: "
                        + f3.startLine());
    }
}
