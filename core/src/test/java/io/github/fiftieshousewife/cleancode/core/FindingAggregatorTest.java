package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FindingAggregatorTest {

    @Test
    void collectsFindingsFromMultipleSources() throws FindingSourceException {
        FindingSource s1 = stubSource("s1", Set.of(HeuristicCode.G5),
                List.of(finding(HeuristicCode.G5, "Foo.java")));
        FindingSource s2 = stubSource("s2", Set.of(HeuristicCode.T1),
                List.of(finding(HeuristicCode.T1, "Bar.java")));

        AggregatedReport report = FindingAggregator.aggregate(
                List.of(s1, s2), dummyContext());

        assertEquals(2, report.findings().size());
    }

    @Test
    void skipsUnavailableSources() throws FindingSourceException {
        FindingSource available = stubSource("a", Set.of(HeuristicCode.G5),
                List.of(finding(HeuristicCode.G5, "Foo.java")));
        FindingSource unavailable = new FindingSource() {
            @Override public String id() { return "u"; }
            @Override public String displayName() { return "U"; }
            @Override public List<Finding> collectFindings(final ProjectContext ctx) { return List.of(finding(HeuristicCode.T1, "X.java")); }
            @Override public Set<HeuristicCode> coveredCodes() { return Set.of(HeuristicCode.T1); }
            @Override public boolean isAvailable(final ProjectContext ctx) { return false; }
        };

        AggregatedReport report = FindingAggregator.aggregate(
                List.of(available, unavailable), dummyContext());

        assertEquals(1, report.findings().size());
    }

    @Test
    void buildsCoveredCodesFromAllSources() throws FindingSourceException {
        FindingSource s1 = stubSource("s1", Set.of(HeuristicCode.G5, HeuristicCode.G9), List.of());
        FindingSource s2 = stubSource("s2", Set.of(HeuristicCode.T1), List.of());

        AggregatedReport report = FindingAggregator.aggregate(
                List.of(s1, s2), dummyContext());

        assertTrue(report.coveredCodes().containsAll(Set.of(HeuristicCode.G5, HeuristicCode.G9, HeuristicCode.T1)));
    }

    @Test
    void handlesEmptySourceList() throws FindingSourceException {
        AggregatedReport report = FindingAggregator.aggregate(List.of(), dummyContext());

        assertTrue(report.findings().isEmpty());
        assertTrue(report.coveredCodes().isEmpty());
    }

    @Test
    void wrapsSourceExceptions() {
        FindingSource failing = new FindingSource() {
            @Override public String id() { return "fail"; }
            @Override public String displayName() { return "Fail"; }
            @Override public List<Finding> collectFindings(final ProjectContext ctx) throws FindingSourceException {
                throw new FindingSourceException("boom");
            }
            @Override public Set<HeuristicCode> coveredCodes() { return Set.of(); }
        };

        assertThrows(FindingSourceException.class, () ->
                FindingAggregator.aggregate(List.of(failing), dummyContext()));
    }

    @Test
    void aggregateWithStatesReportsAllSourceOutcomes() throws FindingSourceException {
        FindingSource produced = stubSource("p", Set.of(HeuristicCode.G5),
                List.of(finding(HeuristicCode.G5, "Foo.java")));
        FindingSource ranEmpty = stubSource("e", Set.of(HeuristicCode.G6), List.of());
        FindingSource unavailable = new FindingSource() {
            @Override public String id() { return "u"; }
            @Override public String displayName() { return "U"; }
            @Override public List<Finding> collectFindings(final ProjectContext ctx) { return List.of(); }
            @Override public Set<HeuristicCode> coveredCodes() { return Set.of(HeuristicCode.T1); }
            @Override public boolean isAvailable(final ProjectContext ctx) { return false; }
        };

        FindingAggregator.Result result = FindingAggregator.aggregateWithStates(
                List.of(produced, ranEmpty, unavailable), dummyContext());

        assertEquals(3, result.sourceStates().size());
        assertEquals(SourceState.Status.PRODUCED_FINDINGS, result.sourceStates().get(0).status());
        assertEquals(SourceState.Status.RAN_NO_FINDINGS, result.sourceStates().get(1).status());
        assertEquals(SourceState.Status.NOT_AVAILABLE, result.sourceStates().get(2).status());
    }

    @Test
    void setsProjectMetadata() throws FindingSourceException {
        AggregatedReport report = FindingAggregator.aggregate(List.of(), dummyContext());

        assertEquals("test-project", report.projectName());
        assertEquals("1.0", report.projectVersion());
        assertNotNull(report.generatedAt());
    }

    private static Finding finding(final HeuristicCode code, final String file) {
        return Finding.at(code, file, 1, 1, "msg", Severity.WARNING, Confidence.HIGH, "test", "rule");
    }

    private static FindingSource stubSource(final String id, final Set<HeuristicCode> codes, final List<Finding> findings) {
        return new FindingSource() {
            @Override public String id() { return id; }
            @Override public String displayName() { return id; }
            @Override public List<Finding> collectFindings(final ProjectContext ctx) { return findings; }
            @Override public Set<HeuristicCode> coveredCodes() { return codes; }
        };
    }

    private static ProjectContext dummyContext() {
        return new ProjectContext(Path.of("/project"), "test-project", "1.0", "21",
                List.of(), List.of(), Path.of("build"), Path.of("build/reports"), List.of());
    }
}
