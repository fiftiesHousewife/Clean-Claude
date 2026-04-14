package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuildOutputFormatterTest {

    @Test
    void formatsEmptyReportWithNoViolationsMessage() {
        final AggregatedReport report = new AggregatedReport(
                List.of(), Set.of(), Instant.now(), "test-project", "1.0");

        final String output = BuildOutputFormatter.format(report);

        assertAll(
                () -> assertTrue(output.contains("CLEAN CODE ANALYSIS")),
                () -> assertTrue(output.contains("test-project")),
                () -> assertTrue(output.contains("No violations found"))
        );
    }

    @Test
    void formatsReportWithFindingsGroupedByCode() {
        final Finding f1 = Finding.at(HeuristicCode.F3, "Foo.java", 10, 10,
                "Boolean parameter 'verbose'", Severity.WARNING, Confidence.HIGH, "openrewrite", "F3");
        final Finding f2 = Finding.at(HeuristicCode.G36, "Bar.java", 20, 20,
                "Method chain depth 4", Severity.WARNING, Confidence.HIGH, "openrewrite", "G36");
        final Finding f3 = Finding.projectLevel(HeuristicCode.T1,
                "Coverage 42%", Severity.ERROR, Confidence.HIGH, "jacoco", "line-coverage");

        final AggregatedReport report = new AggregatedReport(
                List.of(f1, f2, f3), Set.of(HeuristicCode.F3, HeuristicCode.G36, HeuristicCode.T1),
                Instant.now(), "my-project", "1.0");

        final String output = BuildOutputFormatter.format(report);

        assertAll(
                () -> assertTrue(output.contains("my-project")),
                () -> assertTrue(output.contains("1 errors")),
                () -> assertTrue(output.contains("2 warnings")),
                () -> assertTrue(output.contains("F3 (1)")),
                () -> assertTrue(output.contains("G36 (1)")),
                () -> assertTrue(output.contains("T1 (1)")),
                () -> assertTrue(output.contains("Boolean parameter")),
                () -> assertTrue(output.contains("3 findings")),
                () -> assertTrue(output.contains("cleanCodeExplain"))
        );
    }

    @Test
    void includesToolSummary() {
        final Finding f1 = Finding.at(HeuristicCode.F3, "Foo.java", -1, -1,
                "flag arg", Severity.WARNING, Confidence.HIGH, "openrewrite", "F3");
        final Finding f2 = Finding.at(HeuristicCode.J1, "Foo.java", 1, 1,
                "star import", Severity.WARNING, Confidence.HIGH, "checkstyle", "AvoidStarImport");

        final AggregatedReport report = new AggregatedReport(
                List.of(f1, f2), Set.of(), Instant.now(), "test", "1.0");

        final String output = BuildOutputFormatter.format(report);

        assertAll(
                () -> assertTrue(output.contains("openrewrite: 1")),
                () -> assertTrue(output.contains("checkstyle: 1"))
        );
    }
}
