package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableUsagePatternsTest {

    @Test
    void readDetectsPlainIdentifier() {
        assertTrue(VariableUsagePatterns.isRead("int x = count + 1;", "count"));
    }

    @Test
    void readIsWordBoundaryAware() {
        assertAll(
                () -> assertFalse(VariableUsagePatterns.isRead("countryCode = 1;", "count"),
                        "prefix in a longer identifier is not a read"),
                () -> assertFalse(VariableUsagePatterns.isRead("accountId = 1;", "count"),
                        "substring inside a longer identifier is not a read"));
    }

    @Test
    void writeDetectsSimpleAssignment() {
        assertTrue(VariableUsagePatterns.isWritten("total = sum(values);", "total"));
    }

    @Test
    void writeDistinguishesAssignmentFromComparison() {
        assertAll(
                () -> assertFalse(VariableUsagePatterns.isWritten("if (x == 1) { }", "x")),
                () -> assertFalse(VariableUsagePatterns.isWritten("boolean b = x != 2;", "x")));
    }

    @Test
    void writeDetectsCompoundAssignments() {
        assertAll(
                () -> assertTrue(VariableUsagePatterns.isWritten("count += 1;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("count -= 1;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("count *= 2;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("mask |= flag;", "mask")),
                () -> assertTrue(VariableUsagePatterns.isWritten("bits <<= 1;", "bits")));
    }

    @Test
    void writeDetectsIncrementAndDecrement() {
        assertAll(
                () -> assertTrue(VariableUsagePatterns.isWritten("count++;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("--count;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("++count;", "count")),
                () -> assertTrue(VariableUsagePatterns.isWritten("count--;", "count")));
    }

    @Test
    void writeIgnoresAssignmentToDifferentName() {
        assertFalse(VariableUsagePatterns.isWritten("other = count;", "count"),
                "reading `count` into another variable is not a write to `count`");
    }
}
