package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class SummaryLines {

    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────────────────";

    private SummaryLines() {}

    static void appendSeverity(final StringBuilder out, final AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        out.append('\n');
        out.append("  ").append(countOf(bySeverity, Severity.ERROR)).append(" errors");
        out.append("  ·  ").append(countOf(bySeverity, Severity.WARNING)).append(" warnings");
        out.append("  ·  ").append(countOf(bySeverity, Severity.INFO)).append(" info\n");
    }

    static void appendBaselineDelta(final StringBuilder out,
                                    final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final int newViolations = sumChange(deltas, 1);
        final int fixedViolations = sumChange(deltas, -1);

        out.append("  vs baseline: ");
        if (newViolations > 0) {
            out.append('+').append(newViolations).append(" new");
        }
        if (newViolations > 0 && fixedViolations > 0) {
            out.append("  ·  ");
        }
        if (fixedViolations > 0) {
            out.append('-').append(fixedViolations).append(" fixed");
        }
        if (newViolations == 0 && fixedViolations == 0) {
            out.append("no change");
        }
        out.append('\n');
    }

    static void appendToolSummary(final StringBuilder out, final List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));

        out.append('\n').append(DIVIDER).append('\n');
        out.append("  Sources:\n");

        byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                        out.append("    ").append(entry.getKey())
                                .append(": ").append(entry.getValue()).append('\n'));
    }

    private static int countOf(final Map<Severity, List<Finding>> bySeverity, final Severity severity) {
        return bySeverity.getOrDefault(severity, List.of()).size();
    }

    private static int sumChange(final Map<HeuristicCode, BaselineManager.Delta> deltas, final int sign) {
        return deltas.values().stream()
                .mapToInt(d -> Math.max(0, sign * d.change()))
                .sum();
    }
}
