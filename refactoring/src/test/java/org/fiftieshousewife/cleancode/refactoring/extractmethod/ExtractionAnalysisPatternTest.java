package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractionAnalysisPatternTest {

    @Test
    void readDetectsPlainIdentifier() {
        assertTrue(ExtractionAnalysis.isRead("int x = count + 1;", "count"));
    }

    @Test
    void readIsWordBoundaryAware() {
        assertAll(
                () -> assertFalse(ExtractionAnalysis.isRead("countryCode = 1;", "count"),
                        "prefix in a longer identifier is not a read"),
                () -> assertFalse(ExtractionAnalysis.isRead("accountId = 1;", "count"),
                        "substring inside a longer identifier is not a read"));
    }

    @Test
    void writeDetectsSimpleAssignment() {
        assertTrue(ExtractionAnalysis.isWritten("total = sum(values);", "total"));
    }

    @Test
    void writeDistinguishesAssignmentFromComparison() {
        assertAll(
                () -> assertFalse(ExtractionAnalysis.isWritten("if (x == 1) { }", "x")),
                () -> assertFalse(ExtractionAnalysis.isWritten("boolean b = x != 2;", "x")));
    }

    @Test
    void writeDetectsCompoundAssignments() {
        assertAll(
                () -> assertTrue(ExtractionAnalysis.isWritten("count += 1;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("count -= 1;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("count *= 2;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("mask |= flag;", "mask")),
                () -> assertTrue(ExtractionAnalysis.isWritten("bits <<= 1;", "bits")));
    }

    @Test
    void writeDetectsIncrementAndDecrement() {
        assertAll(
                () -> assertTrue(ExtractionAnalysis.isWritten("count++;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("--count;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("++count;", "count")),
                () -> assertTrue(ExtractionAnalysis.isWritten("count--;", "count")));
    }

    @Test
    void writeIgnoresAssignmentToDifferentName() {
        assertFalse(ExtractionAnalysis.isWritten("other = count;", "count"),
                "reading `count` into another variable is not a write to `count`");
    }
}
