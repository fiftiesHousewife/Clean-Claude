package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BaselineManagerTest {

    @Test
    void writesBaselineSnapshot(@TempDir Path tempDir) throws Exception {
        Path baseline = tempDir.resolve("baseline.json");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G5, "Bar.java", 1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G4, "Baz.java", 1, 1, "e", Severity.ERROR, Confidence.HIGH, "pmd", "r")
        );

        BaselineManager.writeBaseline(report, baseline);

        assertTrue(Files.exists(baseline));
        String json = Files.readString(baseline);
        assertTrue(json.contains("G5"));
        assertTrue(json.contains("G4"));
    }

    @Test
    void readsExistingBaseline(@TempDir Path tempDir) throws Exception {
        Path baseline = tempDir.resolve("baseline.json");
        Files.writeString(baseline, """
                {"counts":{"G5":5,"G4":2}}
                """);

        Map<HeuristicCode, Integer> counts = BaselineManager.readBaseline(baseline);

        assertEquals(5, counts.get(HeuristicCode.G5));
        assertEquals(2, counts.get(HeuristicCode.G4));
    }

    @Test
    void computesDelta(@TempDir Path tempDir) throws Exception {
        Path baseline = tempDir.resolve("baseline.json");
        Files.writeString(baseline, """
                {"counts":{"G5":5,"G4":2}}
                """);

        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G5, "Bar.java", 1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G4, "Baz.java", 1, 1, "e", Severity.ERROR, Confidence.HIGH, "pmd", "r"),
                Finding.at(HeuristicCode.G4, "Qux.java", 1, 1, "e", Severity.ERROR, Confidence.HIGH, "pmd", "r"),
                Finding.at(HeuristicCode.G4, "Zap.java", 1, 1, "e", Severity.ERROR, Confidence.HIGH, "pmd", "r")
        );

        Map<HeuristicCode, BaselineManager.Delta> deltas = BaselineManager.computeDeltas(report, baseline);

        assertEquals(-3, deltas.get(HeuristicCode.G5).change()); // 5 -> 2
        assertEquals(1, deltas.get(HeuristicCode.G4).change());  // 2 -> 3
    }

    @Test
    void handlesMissingBaseline(@TempDir Path tempDir) throws Exception {
        Path baseline = tempDir.resolve("nonexistent.json");

        Map<HeuristicCode, Integer> counts = BaselineManager.readBaseline(baseline);

        assertTrue(counts.isEmpty());
    }

    @Test
    void roundTripPreservesCounts(@TempDir Path tempDir) throws Exception {
        Path baseline = tempDir.resolve("baseline.json");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.T1, "Bar.java", 1, 1, "c", Severity.ERROR, Confidence.HIGH, "jacoco", "r")
        );

        BaselineManager.writeBaseline(report, baseline);
        Map<HeuristicCode, Integer> counts = BaselineManager.readBaseline(baseline);

        assertEquals(1, counts.get(HeuristicCode.G5));
        assertEquals(1, counts.get(HeuristicCode.T1));
    }

    private AggregatedReport reportWith(Finding... findings) {
        return new AggregatedReport(List.of(findings), Set.of(), Instant.now(), "test", "1.0");
    }
}
