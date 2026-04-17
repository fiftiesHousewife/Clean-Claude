package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FixBriefGeneratorTest {

    @TempDir
    Path outputDir;

    @Test
    void generatesOneBriefPerFilePlusIndex() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Foo.java",
                        10, 10, "missing final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"),
                Finding.at(HeuristicCode.G30, "core/src/main/java/com/example/Foo.java",
                        20, 30, "method too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"),
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Bar.java",
                        15, 15, "missing final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        final List<Path> written = FixBriefGenerator.generate(report, outputDir);

        assertEquals(3, written.size(), "one brief per file + _INDEX.md");
        final Path fooBrief = outputDir.resolve("Foo.md");
        final Path barBrief = outputDir.resolve("Bar.md");
        assertTrue(Files.exists(fooBrief));
        assertTrue(Files.exists(barBrief));
        assertTrue(Files.exists(outputDir.resolve("_INDEX.md")));

        final String fooContent = Files.readString(fooBrief);
        assertAll(
                () -> assertTrue(fooContent.contains("G22"), "G22 section present"),
                () -> assertTrue(fooContent.contains("G30"), "G30 section present"),
                () -> assertTrue(fooContent.contains("L10"), "line reference present"),
                () -> assertTrue(fooContent.contains("L20-30"), "line range present"),
                () -> assertTrue(fooContent.contains("2 findings"), "count header present"),
                () -> assertTrue(fooContent.contains("Never degrade readability"),
                        "quality anti-goal present"));
    }

    @Test
    void routesFindingToSkillWhenRegistered() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.F1, "core/src/main/java/com/example/Foo.java",
                        10, 10, "too many args", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String content = Files.readString(outputDir.resolve("Foo.md"));
        assertTrue(content.contains(".claude/skills/clean-code-functions/SKILL.md"),
                "F1 should point at clean-code-functions/SKILL.md");
    }

    @Test
    void projectLevelFindingsWriteToStableFileName() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.E1, null,
                        0, 0, "outdated dep", Severity.WARNING, Confidence.HIGH, "benmanes", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final Path brief = outputDir.resolve("project-level-findings.md");
        assertAll(
                () -> assertTrue(Files.exists(brief),
                        "project-level brief uses a non-underscore name so drivers do not skip it"),
                () -> assertFalse(Files.exists(outputDir.resolve("_project-level.md")),
                        "legacy underscore-prefixed name must not be used"));
    }

    @Test
    void indexLinksEveryBrief() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Foo.java",
                        10, 10, "m", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"),
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Bar.java",
                        10, 10, "m", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String index = Files.readString(outputDir.resolve("_INDEX.md"));
        assertAll(
                () -> assertTrue(index.contains("[Foo.md](Foo.md)")),
                () -> assertTrue(index.contains("[Bar.md](Bar.md)")));
    }

    private static AggregatedReport reportWith(final Finding... findings) {
        final Set<HeuristicCode> covered = EnumSet.noneOf(HeuristicCode.class);
        for (final Finding f : findings) {
            covered.add(f.code());
        }
        return new AggregatedReport(List.of(findings), covered,
                Instant.now(), "test-project", "0");
    }
}
