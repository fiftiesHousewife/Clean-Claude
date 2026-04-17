package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class DeltaTableWriter {

    private static final String HEADING = "## Current standing vs baseline\n\n";
    private static final String TABLE_HEADER = "| Category | Baseline | Current | Delta |\n";
    private static final String TABLE_DIVIDER = "|---|---|---|---|\n";
    private static final String ROW_FORMAT = "| %s | %d | %d | %s |%n";

    private DeltaTableWriter() {}

    static void append(final StringBuilder sb, final AggregatedReport report,
                       final Path baselineFile) throws IOException {
        if (baselineFile == null || !Files.exists(baselineFile)) {
            return;
        }
        final Map<HeuristicCode, BaselineManager.Delta> deltas =
                BaselineManager.computeDeltas(report, baselineFile);
        if (deltas.isEmpty()) {
            return;
        }
        appendTableHeader(sb);
        appendTableRows(sb, deltas);
        sb.append('\n');
    }

    private static void appendTableHeader(final StringBuilder sb) {
        sb.append(HEADING);
        sb.append(TABLE_HEADER);
        sb.append(TABLE_DIVIDER);
    }

    private static void appendTableRows(final StringBuilder sb,
                                        final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        deltas.forEach((code, delta) -> sb.append(formatRow(code, delta)));
    }

    private static String formatRow(final HeuristicCode code, final BaselineManager.Delta delta) {
        return String.format(ROW_FORMAT, code.name(), delta.baseline(), delta.current(), formatDelta(delta.change()));
    }

    private static String formatDelta(final long delta) {
        return switch (Long.signum(delta)) {
            case 0 -> "0";
            case 1 -> "+" + delta + " \u26A0";
            default -> delta + " \u2713";
        };
    }
}
