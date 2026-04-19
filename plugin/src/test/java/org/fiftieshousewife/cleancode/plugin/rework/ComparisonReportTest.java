package org.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonReportTest {

    @Test
    void rendersCostTableFindingsTableAndPerVariantSections() {
        final ReworkReport vanillaReport = report(new AgentUsage(
                1000, 500, 20000, 5000, 60_000L, 8, 1.23));
        final ReworkReport mcpReport = report(new AgentUsage(
                1100, 600, 25000, 6000, 70_000L, 9, 1.45));
        final List<ComparisonReport.VariantRun> runs = List.of(
                new ComparisonReport.VariantRun(
                        RunVariant.VANILLA, vanillaReport, "diff-a",
                        new FindingsSnapshot(10, 7, 1, 4)),
                new ComparisonReport.VariantRun(
                        RunVariant.MCP_RECIPES, mcpReport, "diff-b",
                        new FindingsSnapshot(10, 8, 0, 2)));

        final String markdown = ComparisonReport.format(runs);

        assertAll(
                () -> assertTrue(markdown.contains("## Cost"),
                        "missing cost header in:\n" + markdown),
                () -> assertTrue(markdown.contains("## Findings"),
                        "missing findings header in:\n" + markdown),
                () -> assertTrue(markdown.contains("| baseline | 10 | 10 |"),
                        "missing baseline row in:\n" + markdown),
                () -> assertTrue(markdown.contains("| fixed | 7 | 8 |"),
                        "missing fixed row in:\n" + markdown),
                () -> assertTrue(markdown.contains("| introduced | 1 | 0 |"),
                        "missing introduced row in:\n" + markdown),
                () -> assertTrue(markdown.contains("| final | 4 | 2 |"),
                        "missing final row in:\n" + markdown),
                () -> assertTrue(markdown.contains("vanilla — commit message body")),
                () -> assertTrue(markdown.contains("mcp + recipes — commit message body")),
                () -> assertTrue(markdown.contains("diff-a")),
                () -> assertTrue(markdown.contains("diff-b")));
    }

    private static ReworkReport report(final AgentUsage usage) {
        return new ReworkReport(
                List.of(Path.of("sandbox/src/main/java/demo/A.java")),
                ReworkMode.AGENT_DRIVEN,
                List.of(),
                List.of(),
                List.of(),
                Optional.of(usage),
                "body");
    }
}
