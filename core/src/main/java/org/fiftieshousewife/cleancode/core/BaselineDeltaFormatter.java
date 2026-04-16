package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class BaselineDeltaFormatter {

    private BaselineDeltaFormatter() {}

    static void append(final StringBuilder out,
                       final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final int newViolations = sumPositive(deltas);
        final int fixedViolations = sumNegated(deltas);

        out.append("  vs baseline: ");
        if (newViolations > 0) {
            out.append("+").append(newViolations).append(" new");
        }
        if (newViolations > 0 && fixedViolations > 0) {
            out.append("  ·  ");
        }
        if (fixedViolations > 0) {
            out.append("-").append(fixedViolations).append(" fixed");
        }
        if (newViolations == 0 && fixedViolations == 0) {
            out.append("no change");
        }
        out.append('\n');
    }

    private static int sumPositive(final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        return deltas.values().stream()
                .mapToInt(d -> Math.max(0, d.change()))
                .sum();
    }

    private static int sumNegated(final Map<HeuristicCode, BaselineManager.Delta> deltas) {
        return deltas.values().stream()
                .mapToInt(d -> Math.max(0, -d.change()))
                .sum();
    }
}
