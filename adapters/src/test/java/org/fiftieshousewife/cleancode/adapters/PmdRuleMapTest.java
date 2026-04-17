package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PmdRuleMapTest {

    @Test
    void parsesWellFormedEntry() {
        final PmdRuleMap.Entry entry = PmdRuleMap.parseEntry(
                "CyclomaticComplexity",
                "G30|WARNING|MEDIUM|DS|cyclomaticcomplexity");

        assertAll(
                () -> assertEquals(HeuristicCode.G30, entry.code()),
                () -> assertEquals(Severity.WARNING, entry.severity()),
                () -> assertEquals(Confidence.MEDIUM, entry.confidence()),
                () -> assertEquals(
                        "https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity",
                        entry.ruleUrl()));
    }

    @Test
    void rejectsEntryWithWrongFieldCount() {
        final IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> PmdRuleMap.parseEntry("SomeRule", "G30|WARNING|MEDIUM|DS"));

        assertTrue(ex.getMessage().contains("SomeRule"));
    }

    @Test
    void rejectsEntryWithUnknownCategory() {
        final IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> PmdRuleMap.parseEntry("SomeRule", "G30|WARNING|MEDIUM|XX|slug"));

        assertTrue(ex.getMessage().contains("XX"));
    }
}
