package io.github.fiftieshousewife.cleancode.refactoring.extractmethod;

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

    /**
     * Byte offset where the given 1-based line begins (inclusive). Used by the
     * textual-splice path to compute line-aligned boundaries when inserting
     * generated source.
     */
    int startOfLine(final int oneBasedLine) {
        final int idx = Math.max(0, Math.min(oneBasedLine - 1, lineStartOffsets.size() - 1));
        return lineStartOffsets.get(idx);
    }

    /**
     * Byte offset just past the newline terminating the given 1-based line.
     * For the last line without a trailing newline this returns the total
     * text length.
     */
    int endOfLine(final int oneBasedLine) {
        if (oneBasedLine >= lineStartOffsets.size()) {
            return textLength;
        }
        return lineStartOffsets.get(oneBasedLine);
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
