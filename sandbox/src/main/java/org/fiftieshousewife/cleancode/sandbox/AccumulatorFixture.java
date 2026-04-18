package org.fiftieshousewife.cleancode.sandbox;

import java.util.List;

/**
 * Phase B + Phase G fixture. {@link #computeScore} declares a local
 * {@code total} before a for-each loop, the loop writes to
 * {@code total} from inside a {@code continue}-bearing body, and the
 * value of {@code total} is read after the loop. The right extraction
 * is the loop alone — an outer-local reassignment output (Phase B)
 * whose body contains a loop-internal continue (Phase G).
 */
public final class AccumulatorFixture {

    public int computeScore(final List<Integer> values) {
        int total = 0;
        for (final Integer value : values) {
            if (value == null) {
                continue;
            }
            if (value < 0) {
                continue;
            }
            total = total + value;
        }
        if (total > 10000) {
            total = 10000;
        }
        return total;
    }
}
