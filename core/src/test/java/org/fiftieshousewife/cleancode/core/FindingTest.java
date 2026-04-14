package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FindingTest {

    @Test
    void factoryMethodAt_createsFullFinding() {
        Finding f = Finding.at(HeuristicCode.G5, "src/Foo.java", 10, 20,
                "duplicated block", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-rule");

        assertEquals(HeuristicCode.G5, f.code());
        assertEquals("src/Foo.java", f.sourceFile());
        assertEquals(10, f.startLine());
        assertEquals(20, f.endLine());
        assertEquals("duplicated block", f.message());
        assertEquals(Severity.WARNING, f.severity());
        assertEquals(Confidence.HIGH, f.confidence());
        assertEquals("cpd", f.tool());
        assertEquals("cpd-rule", f.ruleRef());
        assertTrue(f.metadata().isEmpty());
    }

    @Test
    void factoryMethodProjectLevel_createsProjectFinding() {
        Finding f = Finding.projectLevel(HeuristicCode.T1, "Low coverage",
                Severity.ERROR, Confidence.HIGH, "jacoco", "coverage");

        assertNull(f.sourceFile());
        assertEquals(-1, f.startLine());
        assertEquals(-1, f.endLine());
        assertEquals(HeuristicCode.T1, f.code());
    }

    @Test
    void findingIsImmutable() {
        Finding f = Finding.at(HeuristicCode.G5, "src/Foo.java", 10, 20,
                "dup", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-rule");

        assertThrows(UnsupportedOperationException.class,
                () -> f.metadata().put("key", "value"));
    }

    @Test
    void findingWithMetadata_preservesMetadata() {
        Map<String, String> meta = Map.of("otherFile", "Bar.java");
        Finding f = new Finding(HeuristicCode.G5, "src/Foo.java", 10, 20,
                "dup", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-rule", meta);

        assertEquals("Bar.java", f.metadata().get("otherFile"));
    }

    @Test
    void twoFindingsWithSameData_areEqual() {
        Finding a = Finding.at(HeuristicCode.G5, "src/Foo.java", 10, 20,
                "dup", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-rule");
        Finding b = Finding.at(HeuristicCode.G5, "src/Foo.java", 10, 20,
                "dup", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-rule");

        assertEquals(a, b);
    }
}
