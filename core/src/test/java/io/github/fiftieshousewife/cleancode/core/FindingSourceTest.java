package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FindingSourceTest {

    @Test
    void defaultIsAvailable_returnsTrue() {
        FindingSource stub = new FindingSource() {
            @Override public String id() { return "stub"; }
            @Override public String displayName() { return "Stub"; }
            @Override public List<Finding> collectFindings(ProjectContext context) { return List.of(); }
            @Override public Set<HeuristicCode> coveredCodes() { return Set.of(); }
        };

        assertTrue(stub.isAvailable(null));
    }

    @Test
    void stubSourceCollectsFindings() throws FindingSourceException {
        Finding expected = Finding.at(HeuristicCode.G5, "Foo.java", 1, 1,
                "test", Severity.INFO, Confidence.HIGH, "stub", "rule");

        FindingSource stub = new FindingSource() {
            @Override public String id() { return "stub"; }
            @Override public String displayName() { return "Stub"; }
            @Override public List<Finding> collectFindings(ProjectContext context) {
                return List.of(expected);
            }
            @Override public Set<HeuristicCode> coveredCodes() { return Set.of(HeuristicCode.G5); }
        };

        List<Finding> findings = stub.collectFindings(null);
        assertEquals(1, findings.size());
        assertEquals(expected, findings.getFirst());
    }
}
