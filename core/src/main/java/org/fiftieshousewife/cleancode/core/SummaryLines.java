package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.BaselineManager.Delta;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.fiftieshousewife.cleancode.core.Severity.ERROR;
import static org.fiftieshousewife.cleancode.core.Severity.INFO;
import static org.fiftieshousewife.cleancode.core.Severity.WARNING;

final class SummaryLines {

    private static final String SEPARATOR = "  ·  ";

    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────────────────";

    private SummaryLines() {}

    static void appendSeverity(final StringBuilder out, final AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        final String line = "\n  " + countOf(bySeverity, ERROR) + " errors"
                + SEPARATOR + countOf(bySeverity, WARNING) + " warnings"
                + SEPARATOR + countOf(bySeverity, INFO) + " info\n";
        out.append(line);
    }

    static void appendBaselineDelta(final StringBuilder out,
                                    final Map<HeuristicCode, Delta> deltas) {
        final int newViolations = sumChange(deltas, 1);
        final int fixedViolations = sumChange(deltas, -1);

        out.append("  vs baseline: ");
        if (newViolations > 0) {
            out.append('+').append(newViolations).append(" new");
        }
        if (newViolations > 0 && fixedViolations > 0) {
            out.append(SEPARATOR);
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

        final String sources = byTool.entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> "    " + entry.getKey() + ": " + entry.getValue() + '\n')
                .collect(Collectors.joining());

        out.append('\n').append(DIVIDER).append('\n').append("  Sources:\n").append(sources);
    }

    private static int countOf(final Map<Severity, List<Finding>> bySeverity, final Severity severity) {
        return bySeverity.getOrDefault(severity, List.of()).size();
    }

    private static int sumChange(final Map<HeuristicCode, Delta> deltas, final int sign) {
        return deltas.values().stream()
                .mapToInt(d -> Math.max(0, sign * d.change()))
                .sum();
    }
}
