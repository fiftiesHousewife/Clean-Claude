package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuppressionIndexTest {

    @Test
    void isSuppressed_trueForFindingWithinAnnotatedMethod() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.G30, "com/example/MethodSuppressed.java",
                10, 10, "complex", Severity.WARNING, Confidence.MEDIUM, "pmd", "rule");

        assertTrue(index.isSuppressed(finding));
    }

    @Test
    void isSuppressed_falseForFindingOutsideAnnotatedMethod() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.G30, "com/example/MethodSuppressed.java",
                15, 15, "complex", Severity.WARNING, Confidence.MEDIUM, "pmd", "rule");

        assertFalse(index.isSuppressed(finding));
    }

    @Test
    void isSuppressed_falseForDifferentCode() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.G5, "com/example/MethodSuppressed.java",
                10, 10, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "rule");

        assertFalse(index.isSuppressed(finding));
    }

    @Test
    void classLevelSuppressionCoversAllFindings() {
        SuppressionIndex index = buildIndex();

        Finding f1 = Finding.at(HeuristicCode.G8, "com/example/ClassSuppressed.java",
                10, 10, "too many", Severity.WARNING, Confidence.HIGH, "pmd", "rule");
        Finding f2 = Finding.at(HeuristicCode.G8, "com/example/ClassSuppressed.java",
                14, 14, "too many", Severity.WARNING, Confidence.HIGH, "pmd", "rule");

        assertTrue(index.isSuppressed(f1));
        assertTrue(index.isSuppressed(f2));
    }

    @Test
    void handlesRepeatableAnnotations() {
        SuppressionIndex index = buildIndex();

        Finding f1 = Finding.at(HeuristicCode.F1, "com/example/RepeatableSuppression.java",
                11, 11, "params", Severity.WARNING, Confidence.HIGH, "checkstyle", "rule");
        Finding f2 = Finding.at(HeuristicCode.G28, "com/example/RepeatableSuppression.java",
                11, 11, "condition", Severity.WARNING, Confidence.MEDIUM, "openrewrite", "rule");

        assertTrue(index.isSuppressed(f1));
        assertTrue(index.isSuppressed(f2));
    }

    @Test
    void expiredUntilDate_notSuppressed_producesMetaFinding() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.F1, "com/example/ExpiredSuppression.java",
                10, 10, "params", Severity.WARNING, Confidence.HIGH, "checkstyle", "rule");

        assertFalse(index.isSuppressed(finding));

        List<Finding> meta = index.metaFindings();
        assertTrue(meta.stream().anyMatch(f ->
                f.code() == HeuristicCode.META_SUPPRESSION_EXPIRED
                        && f.sourceFile().contains("ExpiredSuppression")));
    }

    @Test
    void futureUntilDate_stillSuppressed() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.F1, "com/example/ExpiredSuppression.java",
                15, 15, "params", Severity.WARNING, Confidence.HIGH, "checkstyle", "rule");

        assertTrue(index.isSuppressed(finding));
    }

    @Test
    void blankOrTodoReason_producesMetaFinding() {
        SuppressionIndex index = buildIndex();

        List<Finding> meta = index.metaFindings();
        assertTrue(meta.stream().anyMatch(f ->
                f.code() == HeuristicCode.META_SUPPRESSION_NO_REASON
                        && f.sourceFile().contains("BlankReason")));
    }

    @Test
    void noAnnotations_nothingSuppressed() {
        SuppressionIndex index = buildIndex();

        Finding finding = Finding.at(HeuristicCode.G30, "com/example/NoAnnotations.java",
                6, 6, "complex", Severity.WARNING, Confidence.MEDIUM, "pmd", "rule");

        assertFalse(index.isSuppressed(finding));
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
