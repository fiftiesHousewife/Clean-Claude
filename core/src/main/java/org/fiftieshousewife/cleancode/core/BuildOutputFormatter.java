package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

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
                                final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final StringBuilder out = new StringBuilder();
        final List<Finding> findings = report.findings();

        appendTitle(out, report.projectName());

        if (findings.isEmpty()) {
            out.append("\n  No violations found. The code is clean.\n");
            out.append('\n').append(HEADER).append('\n');
            return out.toString();
        }

        SummaryLines.appendSeverity(out, report);
        if (!deltas.isEmpty()) {
            SummaryLines.appendBaselineDelta(out, deltas);
        }
        FindingsSection.append(out, findings);
        SummaryLines.appendToolSummary(out, findings);
        appendFooter(out, findings);

        return out.toString();
    }

    private static void appendTitle(final StringBuilder out, final String projectName) {
        out.append('\n').append(HEADER).append('\n');
        out.append("  CLEAN CODE ANALYSIS  —  ").append(projectName).append('\n');
        out.append(HEADER).append('\n');
    }

    private static void appendFooter(final StringBuilder out, final List<Finding> findings) {
        out.append('\n').append(HEADER).append('\n');
        out.append("  ").append(findings.size()).append(" findings");
        out.append("  —  ./gradlew cleanCodeExplain --finding=<code>\n");
        out.append(HEADER).append('\n');
    }
}
