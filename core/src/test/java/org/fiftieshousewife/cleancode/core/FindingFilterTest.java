package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FindingFilterTest {

    @Test
    void removesSuppressedFindings() {
        SuppressionIndex index = buildIndex();
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.G30, "com/example/MethodSuppressed.java",
                        10, 10, "complex", Severity.WARNING, Confidence.MEDIUM, "pmd", "rule"));

        FindingFilter.Result result = FindingFilter.apply(findings, index);

        // The suppressed finding should be removed; only meta-findings remain
        assertFalse(result.findings().stream().anyMatch(f -> f.tool().equals("pmd")));
    }

    @Test
    void preservesNonSuppressedFindings() {
        SuppressionIndex index = buildIndex();
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.G30, "com/example/MethodSuppressed.java",
                        15, 15, "complex", Severity.WARNING, Confidence.MEDIUM, "pmd", "rule"));

        FindingFilter.Result result = FindingFilter.apply(findings, index);

        assertTrue(result.findings().stream().anyMatch(f -> f.tool().equals("pmd")));
    }

    @Test
    void appendsMetaFindings() {
        SuppressionIndex index = buildIndex();

        FindingFilter.Result result = FindingFilter.apply(List.of(), index);

        assertFalse(result.findings().isEmpty());
        assertTrue(result.findings().stream().anyMatch(f ->
                f.code() == HeuristicCode.META_SUPPRESSION_EXPIRED
                        || f.code() == HeuristicCode.META_SUPPRESSION_NO_REASON));
    }

    @Test
    void emptySuppressionIndexLeavesAllFindings() {
        SuppressionIndex index = SuppressionIndex.build(Path.of("nonexistent"));
        List<Finding> findings = List.of(
                Finding.at(HeuristicCode.G5, "Foo.java", 1, 1,
                        "dup", Severity.WARNING, Confidence.HIGH, "cpd", "rule"));

        FindingFilter.Result result = FindingFilter.apply(findings, index);

        assertEquals(1, result.findings().size());
    }

    @Test
    void spotBugsFindingsAreNeverFiltered() {
        SuppressionIndex index = buildIndex();
        Finding spotbugs = Finding.at(HeuristicCode.G8, "com/example/ClassSuppressed.java",
                10, 10, "bug", Severity.ERROR, Confidence.HIGH, "spotbugs", "rule");

        FindingFilter.Result result = FindingFilter.apply(List.of(spotbugs), index);

        assertEquals(1, result.findings().stream()
                .filter(f -> "spotbugs".equals(f.tool())).count());
    }

    private SuppressionIndex buildIndex() {
        try {
            Path sourceRoot = Path.of(getClass().getResource("/suppression").toURI());
            return SuppressionIndex.build(sourceRoot);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
