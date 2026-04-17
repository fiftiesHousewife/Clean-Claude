package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.BaselineManager.Delta;

import java.util.List;
import java.util.Map;

public final class BuildOutputFormatter {

    private static final String HEADER =
            "═══════════════════════════════════════════════════════════════════════════";

    private BuildOutputFormatter() {}

    public static String format(final AggregatedReport report) {
        return format(report, Map.of());
    }

    public static String format(final AggregatedReport report,
                                final Map<HeuristicCode, Delta> deltas) {
        final List<Finding> findings = report.findings();
        final String title = title(report.projectName());
        if (findings.isEmpty()) {
            return title + emptyBody();
        }
        return title + body(report, findings, deltas);
    }

    private static String title(final String projectName) {
        return "\n" + HEADER + "\n"
                + "  CLEAN CODE ANALYSIS  —  " + projectName + "\n"
                + HEADER + "\n";
    }

    private static String emptyBody() {
        return "\n  No violations found. The code is clean.\n"
                + "\n" + HEADER + "\n";
    }

    private static String body(final AggregatedReport report,
                               final List<Finding> findings,
                               final Map<HeuristicCode, Delta> deltas) {
        final StringBuilder out = new StringBuilder();
        SummaryLines.appendSeverity(out, report);
        if (!deltas.isEmpty()) {
            SummaryLines.appendBaselineDelta(out, deltas);
        }
        FindingsSection.append(out, findings);
        SummaryLines.appendToolSummary(out, findings);
        out.append(footer(findings));
        return out.toString();
    }

    private static String footer(final List<Finding> findings) {
        return "\n" + HEADER + "\n"
                + "  " + findings.size() + " findings"
                + "  —  ./gradlew cleanCodeExplain --finding=<code>\n"
                + HEADER + "\n";
    }
}
