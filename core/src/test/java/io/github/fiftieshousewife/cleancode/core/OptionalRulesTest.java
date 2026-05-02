package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalRulesTest {

    @Test
    void recognisesFinalLocalVariableAsOptional() {
        final Finding finding = new Finding(
                HeuristicCode.G22, "Foo.java", 10, 10, "make this final",
                Severity.WARNING, Confidence.HIGH, "checkstyle", "FinalLocalVariable", Map.of());

        assertTrue(OptionalRules.isOptional(finding),
                "FinalLocalVariable from Checkstyle should be opt-in by default");
    }

    @Test
    void recognisesLocaleCaseConversionAsOptional() {
        final Finding finding = new Finding(
                HeuristicCode.G26, "Foo.java", 10, 10, "use a Locale",
                Severity.WARNING, Confidence.HIGH, "pmd", "UseLocaleWithCaseConversions", Map.of());

        assertTrue(OptionalRules.isOptional(finding),
                "UseLocaleWithCaseConversions from PMD should be opt-in by default");
    }

    @Test
    void otherFindingsAreNotOptional() {
        final Finding finding = new Finding(
                HeuristicCode.G4, "Foo.java", 10, 10, "empty catch",
                Severity.ERROR, Confidence.HIGH, "checkstyle", "EmptyBlock", Map.of());

        assertFalse(OptionalRules.isOptional(finding));
    }

    @Test
    void disabledByDefaultButEnabledWhenInOptInList() {
        final Finding finalLocal = new Finding(
                HeuristicCode.G22, "Foo.java", 10, 10, "x",
                Severity.WARNING, Confidence.HIGH, "checkstyle", "FinalLocalVariable", Map.of());

        assertAll(
                () -> assertFalse(OptionalRules.isEnabled(finalLocal, Set.of()),
                        "default behaviour: optional rule is disabled"),
                () -> assertTrue(OptionalRules.isEnabled(finalLocal, Set.of("checkstyle:FinalLocalVariable")),
                        "user opted in: optional rule is enabled"),
                () -> assertFalse(OptionalRules.isEnabled(finalLocal, Set.of("pmd:UseLocaleWithCaseConversions")),
                        "opting in to a different rule does not enable this one"));
    }

    @Test
    void nonOptionalFindingsAlwaysPass() {
        final Finding finding = new Finding(
                HeuristicCode.G4, "Foo.java", 10, 10, "empty",
                Severity.ERROR, Confidence.HIGH, "checkstyle", "EmptyBlock", Map.of());

        assertTrue(OptionalRules.isEnabled(finding, Set.of()),
                "non-optional findings are always enabled regardless of the opt-in set");
    }

    @Test
    void defaultsExposesEachRuleWithCodeAndHowToEnable() {
        final OptionalRules.OptionalRule rule = OptionalRules.defaults().get("checkstyle:FinalLocalVariable");

        assertAll(
                () -> assertTrue(rule.code().equals("G22")),
                () -> assertTrue(rule.summary().toLowerCase().contains("final"),
                        "summary should mention what the rule enforces"),
                () -> assertTrue(rule.howToEnable().contains("enabledOptionalRules"),
                        "howToEnable should reference the property the user must set"));
    }
}
