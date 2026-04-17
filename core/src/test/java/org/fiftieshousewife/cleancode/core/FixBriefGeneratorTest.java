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
        assertAll(
                () -> assertTrue(content.contains(".claude/skills/clean-code-functions/SKILL.md"),
                        "F1 should point at clean-code-functions/SKILL.md"),
                () -> assertTrue(content.contains("You MUST Read this file first"),
                        "brief must order the agent to read the skill before editing"),
                () -> assertTrue(content.contains("Your first tool calls MUST be Reads"),
                        "top-of-brief preamble must tell the agent to read every cited skill first"));
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
    void siblingBlockListsOtherJavaTypesInTheSameDirectory(@TempDir Path projectRoot) throws IOException {
        final Path pkg = projectRoot.resolve("core/src/main/java/com/example");
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("Foo.java"), "package com.example; public class Foo {}");
        Files.writeString(pkg.resolve("Bar.java"), "package com.example; public class Bar {}");
        Files.writeString(pkg.resolve("Baz.java"), "package com.example; public class Baz {}");

        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Foo.java",
                        10, 10, "missing final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        FixBriefGenerator.generate(report, outputDir, projectRoot);

        final String content = Files.readString(outputDir.resolve("Foo.md"));
        assertAll(
                () -> assertTrue(content.contains("Sibling types in this package"),
                        "brief must include the sibling-context header"),
                () -> assertTrue(content.contains("- Bar"),
                        "Bar sibling must be listed"),
                () -> assertTrue(content.contains("- Baz"),
                        "Baz sibling must be listed"),
                () -> assertFalse(content.contains("- Foo"),
                        "the file itself must not be listed as its own sibling"));
    }

    @Test
    void siblingBlockOmittedWhenProjectRootIsUnknown() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G22, "core/src/main/java/com/example/Foo.java",
                        10, 10, "missing final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String content = Files.readString(outputDir.resolve("Foo.md"));
        assertFalse(content.contains("Sibling types in this package"),
                "without a projectRoot we cannot enumerate siblings — omit the block entirely");
    }

    @Test
    void metricSqueezingWarningAppearsWhenSizeAndDuplicationBothPresent() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.Ch10_1, "core/src/main/java/com/example/Big.java",
                        1, 200, "class too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"),
                Finding.at(HeuristicCode.G5, "core/src/main/java/com/example/Big.java",
                        40, 80, "duplicate block", Severity.WARNING, Confidence.HIGH, "cpd", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String content = Files.readString(outputDir.resolve("Big.md"));
        assertAll(
                () -> assertTrue(content.contains("Do not metric-squeeze"),
                        "brief with Ch10.1 + G5 must call out the squeeze anti-pattern"),
                () -> assertTrue(content.contains("Split by responsibility, not by LOC"),
                        "brief must give the positive replacement for LOC-minimisation"));
    }

    @Test
    void metricSqueezingWarningAbsentWhenOnlySizeFindingPresent() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.Ch10_1, "core/src/main/java/com/example/Big.java",
                        1, 200, "class too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String content = Files.readString(outputDir.resolve("Big.md"));
        assertFalse(content.contains("Do not metric-squeeze"),
                "size finding alone should not trigger the squeeze warning");
    }

    @Test
    void e1SectionOrdersAgentToBumpAndRunTests() throws IOException {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.E1, null,
                        0, 0, "outdated dep", Severity.WARNING, Confidence.HIGH, "benmanes", "r"));

        FixBriefGenerator.generate(report, outputDir);

        final String content = Files.readString(outputDir.resolve("project-level-findings.md"));
        assertAll(
                () -> assertTrue(content.contains("gradle/libs.versions.toml"),
                        "E1 section must point at the file the agent should edit"),
                () -> assertTrue(content.contains("one commit per dep"),
                        "E1 section must instruct per-dep commits"),
                () -> assertTrue(content.contains("major-version jump"),
                        "E1 section must carve out major-version bumps as the only legitimate skip"),
                () -> assertFalse(content.contains("SuppressCleanCode"),
                        "E1 section must not mention suppression — action, not avoidance"));
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
