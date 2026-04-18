package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps zero-based character offsets in a source text to 1-based line numbers.
 * Used by the extract-method recipe to convert the AST's printed form back
 * to the line coordinates that {@code ExtractMethodRecipe}'s callers supply.
 */
final class LineIndex {

    private final List<Integer> lineStartOffsets;
    private final int textLength;

    private LineIndex(final List<Integer> lineStartOffsets, final int textLength) {
        this.lineStartOffsets = lineStartOffsets;
        this.textLength = textLength;
    }

    static LineIndex forText(final String text) {
        final List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        return new LineIndex(Collections.unmodifiableList(starts), text.length());
    }

    int lineOf(final int offset) {
        final int clamped = Math.max(0, Math.min(offset, textLength - 1));
        int lo = 0;
        int hi = lineStartOffsets.size() - 1;
        while (lo < hi) {
            final int mid = (lo + hi + 1) >>> 1;
            if (lineStartOffsets.get(mid) <= clamped) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo + 1;
    }
}
