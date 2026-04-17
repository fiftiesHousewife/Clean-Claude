package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BuildOutputFormatter {

    private static final String HEADER =
            "═══════════════════════════════════════════════════════════════════════════";
    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────────────────";
    private static final String METRIC_SEPARATOR = "  ·  ";

    private BuildOutputFormatter() {}

    public static String format(final AggregatedReport report) {
        return format(report, Map.of());
    }

    public static String format(final AggregatedReport report,
                                final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final StringBuilder out = new StringBuilder();
        appendBanner(out, report.projectName());
        if (report.findings().isEmpty()) {
            return appendNoViolations(out).toString();
        }
        return appendFindings(out, report, deltas).toString();
    }

    private static StringBuilder appendNoViolations(final StringBuilder out) {
        return out.append("\n  No violations found. The code is clean.\n")
                .append('\n').append(HEADER).append('\n');
    }

    private static StringBuilder appendFindings(final StringBuilder out,
                                                final AggregatedReport report,
                                                final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final List<Finding> findings = report.findings();
        appendSeveritySummary(out, report);
        if (!deltas.isEmpty()) {
            BaselineDeltaFormatter.append(out, deltas);
        }
        FindingsByCodeFormatter.append(out, findings);
        appendToolSummary(out, findings);
        appendFooter(out, findings);
        return out;
    }

    private static void appendBanner(final StringBuilder out, final String projectName) {
        out.append('\n').append(HEADER).append('\n')
                .append("  CLEAN CODE ANALYSIS  —  ").append(projectName).append('\n')
                .append(HEADER).append('\n');
    }

    private static void appendSeveritySummary(final StringBuilder out, final AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        out.append('\n')
                .append("  ").append(severityCount(bySeverity, Severity.ERROR)).append(" errors")
                .append(METRIC_SEPARATOR).append(severityCount(bySeverity, Severity.WARNING)).append(" warnings")
                .append(METRIC_SEPARATOR).append(severityCount(bySeverity, Severity.INFO)).append(" info\n");
    }

    private static int severityCount(final Map<Severity, List<Finding>> bySeverity,
                                     final Severity severity) {
        return bySeverity.getOrDefault(severity, List.of()).size();
    }

    private static void appendToolSummary(final StringBuilder out, final List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));
        out.append('\n').append(DIVIDER).append('\n')
                .append("  Sources:\n");
        byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> out.append("    ").append(entry.getKey())
                        .append(": ").append(entry.getValue()).append('\n'));
    }

    private static void appendFooter(final StringBuilder out, final List<Finding> findings) {
        out.append('\n').append(HEADER).append('\n')
                .append("  ").append(findings.size()).append(" findings")
                .append("  —  ./gradlew cleanCodeExplain --finding=<code>\n")
                .append(HEADER).append('\n');
    }
}
