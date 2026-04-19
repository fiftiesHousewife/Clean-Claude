package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackageSuppressionTest {

    @Test
    void suppressesFindingInConfiguredPackage() {
        final PackageSuppression sup = PackageSuppression.of(Map.of(
                "io.github.fiftieshousewife.cleancode.recipes", List.of("G5", "Ch7_2")));
        final Finding hit = Finding.at(HeuristicCode.G5,
                "recipes/src/main/java/io/github/fiftieshousewife/cleancode/recipes/SomeRecipe.java",
                10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "rule");

        assertTrue(sup.suppresses(hit));
    }

    @Test
    void leavesFindingOutsideConfiguredPackage() {
        final PackageSuppression sup = PackageSuppression.of(Map.of(
                "io.github.fiftieshousewife.cleancode.recipes", List.of("G5")));
        final Finding miss = Finding.at(HeuristicCode.G5,
                "core/src/main/java/io/github/fiftieshousewife/cleancode/core/Foo.java",
                10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "rule");

        assertFalse(sup.suppresses(miss));
    }

    @Test
    void leavesFindingWithDifferentCode() {
        final PackageSuppression sup = PackageSuppression.of(Map.of(
                "io.github.fiftieshousewife.cleancode.recipes", List.of("G5")));
        final Finding wrongCode = Finding.at(HeuristicCode.G30,
                "recipes/src/main/java/io/github/fiftieshousewife/cleancode/recipes/SomeRecipe.java",
                10, 20, "long", Severity.WARNING, Confidence.HIGH, "openrewrite", "rule");

        assertFalse(sup.suppresses(wrongCode));
    }

    @Test
    void suppressesCpdFindingWhenOnlyOtherFileMatchesPackage() {
        final PackageSuppression sup = PackageSuppression.of(Map.of(
                "io.github.fiftieshousewife.cleancode.recipes", List.of("G5")));
        final Finding crossFile = new Finding(HeuristicCode.G5,
                "core/src/main/java/io/github/fiftieshousewife/cleancode/core/Foo.java",
                10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-duplication",
                Map.of("otherFile",
                        "recipes/src/main/java/io/github/fiftieshousewife/cleancode/recipes/Bar.java"));

        assertTrue(sup.suppresses(crossFile));
    }

    @Test
    void emptyConfigSuppressesNothing() {
        final PackageSuppression sup = PackageSuppression.empty();
        final Finding any = Finding.at(HeuristicCode.G5, "x/y.java",
                1, 1, "d", Severity.WARNING, Confidence.HIGH, "cpd", "rule");

        assertFalse(sup.suppresses(any));
    }
}
