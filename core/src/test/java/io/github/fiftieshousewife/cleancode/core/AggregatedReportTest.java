package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AggregatedReportTest {

    @Test
    void byCode_groupsFindingsByHeuristicCode() {
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.G5, "Foo.java", 1, 10, "dup1", Severity.WARNING, Confidence.HIGH, "cpd", "r1"),
                Finding.at(HeuristicCode.G5, "Bar.java", 5, 15, "dup2", Severity.WARNING, Confidence.HIGH, "cpd", "r2"),
                Finding.at(HeuristicCode.T1, "Baz.java", 1, 1, "low cov", Severity.ERROR, Confidence.HIGH, "jacoco", "r3")
        );
        AggregatedReport report = new AggregatedReport(findings, Set.of(), Instant.now(), "proj", "1.0");

        Map<HeuristicCode, List<Finding>> byCode = report.byCode();
        assertEquals(2, byCode.size());
        assertEquals(2, byCode.get(HeuristicCode.G5).size());
        assertEquals(1, byCode.get(HeuristicCode.T1).size());
    }

    @Test
    void byCode_returnsTreeMap_sortedByCodeName() {
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.T1, "a.java", 1, 1, "t", Severity.INFO, Confidence.HIGH, "t", "r"),
                Finding.at(HeuristicCode.G5, "b.java", 1, 1, "g", Severity.INFO, Confidence.HIGH, "t", "r"),
                Finding.at(HeuristicCode.C5, "c.java", 1, 1, "c", Severity.INFO, Confidence.HIGH, "t", "r")
        );
        AggregatedReport report = new AggregatedReport(findings, Set.of(), Instant.now(), "proj", "1.0");

        List<HeuristicCode> keys = new ArrayList<>(report.byCode().keySet());
        assertEquals(HeuristicCode.C5, keys.get(0));
        assertEquals(HeuristicCode.G5, keys.get(1));
        assertEquals(HeuristicCode.T1, keys.get(2));
    }

    @Test
    void bySeverity_groupsFindingsBySeverity() {
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.G4, "a.java", 1, 1, "err", Severity.ERROR, Confidence.HIGH, "t", "r"),
                Finding.at(HeuristicCode.G5, "b.java", 1, 1, "warn", Severity.WARNING, Confidence.HIGH, "t", "r"),
                Finding.at(HeuristicCode.G9, "c.java", 1, 1, "info", Severity.INFO, Confidence.HIGH, "t", "r")
        );
        AggregatedReport report = new AggregatedReport(findings, Set.of(), Instant.now(), "proj", "1.0");

        Map<Severity, List<Finding>> bySev = report.bySeverity();
        assertEquals(3, bySev.size());
        assertEquals(1, bySev.get(Severity.ERROR).size());
        assertEquals(1, bySev.get(Severity.WARNING).size());
        assertEquals(1, bySev.get(Severity.INFO).size());
    }

    @Test
    void emptyReport_returnsEmptyMaps() {
        AggregatedReport report = new AggregatedReport(List.of(), Set.of(), Instant.now(), "proj", "1.0");

        assertTrue(report.byCode().isEmpty());
        assertTrue(report.bySeverity().isEmpty());
    }

    @Test
    void coveredCodesIncludesCodesWithNoFindings() {
        Set<HeuristicCode> covered = Set.of(HeuristicCode.G5, HeuristicCode.T1, HeuristicCode.F1);
        AggregatedReport report = new AggregatedReport(List.of(), covered, Instant.now(), "proj", "1.0");

        assertEquals(covered, report.coveredCodes());
    }
}
