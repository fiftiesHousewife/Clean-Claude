package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LineIndexTest {

    @Test
    void firstCharIsOnLineOne() {
        final LineIndex index = LineIndex.forText("alpha\nbeta\ngamma\n");
        assertEquals(1, index.lineOf(0));
    }

    @Test
    void charAfterFirstNewlineIsOnLineTwo() {
        final LineIndex index = LineIndex.forText("alpha\nbeta\ngamma\n");
        assertEquals(2, index.lineOf(6));
    }

    @Test
    void newlineCharItselfCountsAsTerminatingLine() {
        final LineIndex index = LineIndex.forText("alpha\nbeta\n");
        assertEquals(1, index.lineOf(5),
                "offset of the '\\n' after `alpha` is still considered line 1");
    }

    @Test
    void lastCharOfFinalLineResolvesCorrectly() {
        final LineIndex index = LineIndex.forText("a\nbb\nccc");
        assertAll(
                () -> assertEquals(1, index.lineOf(0)),
                () -> assertEquals(2, index.lineOf(2)),
                () -> assertEquals(3, index.lineOf(5)),
                () -> assertEquals(3, index.lineOf(7)));
    }

    @Test
    void offsetPastEndClampsToLastLine() {
        final LineIndex index = LineIndex.forText("a\nb");
        assertEquals(2, index.lineOf(99));
    }

    @Test
    void emptyTextResolvesToLineOne() {
        final LineIndex index = LineIndex.forText("");
        assertEquals(1, index.lineOf(0));
    }
}
