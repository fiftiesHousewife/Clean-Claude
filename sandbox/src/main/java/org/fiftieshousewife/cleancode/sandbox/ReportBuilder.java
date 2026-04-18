package org.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deliberately-smelly fixture for the rework harness. Self-contained
 * (no project-internal dependencies) so the sandbox module compiles on
 * its own; shaped to carry the kinds of findings the rework flow is
 * meant to fix:
 *
 * <ul>
 *   <li>{@link #buildReport} is a long orchestrator method (Ch10.1 /
 *       G30) with clear sub-phases that a human would factor out.</li>
 *   <li>An accumulator pattern on a {@code StringBuilder sb} parameter —
 *       the F2 / sb-threading pattern the manual-1 audit called out.</li>
 *   <li>An early-return guard at the top of {@link #format} — Phase A
 *       of ExtractMethodRecipe should be able to extract it.</li>
 *   <li>An outer-local accumulator inside {@link #tally} — Phase B of
 *       ExtractMethodRecipe should be able to extract the loop.</li>
 *   <li>A catch-log-continue block (Ch7.1).</li>
 *   <li>Magic numbers and repeated string literals (G25 / G35).</li>
 * </ul>
 */
public final class ReportBuilder {

    public String format(final String title, final List<String> rows) {
        if (title == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        appendRows(sb, rows);
        return sb.toString();
    }

    public String buildReport(final String title, final List<String> rows,
                              final Map<String, Integer> counters, final long generatedAt) {
        final StringBuilder out = new StringBuilder();
        out.append("# ").append(title).append("\n");
        out.append("Generated at: ").append(generatedAt).append("\n\n");
        out.append("## Rows\n");
        for (final String row : rows) {
            if (row == null || row.isBlank()) {
                continue;
            }
            out.append("- ").append(row.trim()).append("\n");
        }
        out.append("\n");
        out.append("## Counters\n");
        int totalCount = 0;
        for (final Map.Entry<String, Integer> entry : counters.entrySet()) {
            out.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            totalCount = totalCount + entry.getValue();
        }
        out.append("\n");
        out.append("Total: ").append(totalCount).append("\n");
        out.append("\n");
        out.append("## Summary\n");
        if (totalCount > 100) {
            out.append("- Level: HIGH").append("\n");
        } else if (totalCount > 10) {
            out.append("- Level: MEDIUM").append("\n");
        } else {
            out.append("- Level: LOW").append("\n");
        }
        out.append("- Row count: ").append(rows.size()).append("\n");
        out.append("- Counter count: ").append(counters.size()).append("\n");
        out.append("\n");
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            out.append("(interrupted while finalising)\n");
        }
        return out.toString();
    }

    public int tally(final List<String> rows, final String needle) {
        int count = 0;
        for (final String row : rows) {
            if (row == null) {
                continue;
            }
            if (row.contains(needle)) {
                count = count + 1;
            }
        }
        return count;
    }

    private void appendRows(final StringBuilder sb, final List<String> rows) {
        for (final String row : rows) {
            appendRow(sb, row);
        }
    }

    private void appendRow(final StringBuilder sb, final String row) {
        if (row == null) {
            return;
        }
        sb.append("- ").append(row).append("\n");
    }

    public List<String> classify(final List<Integer> values) {
        final List<String> labels = new ArrayList<>();
        for (final Integer v : values) {
            if (v == null) {
                labels.add("null");
            } else if (v < 0) {
                labels.add("negative");
            } else if (v == 0) {
                labels.add("zero");
            } else if (v < 10) {
                labels.add("small");
            } else if (v < 100) {
                labels.add("medium");
            } else {
                labels.add("large");
            }
        }
        return labels;
    }
}
