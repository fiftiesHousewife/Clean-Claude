package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void vanillaVariantMentionsNoMcpToolsAndFallsBackToManualEdit() {
        final String prompt = PromptBuilder.build("Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), RunVariant.VANILLA);
        assertAll(
                () -> assertFalse(prompt.contains("extract_method("),
                        "vanilla must not advertise the recipe tool"),
                () -> assertFalse(prompt.contains("verify_build("),
                        "vanilla must not advertise the gradle MCP tools"),
                () -> assertTrue(prompt.contains("No MCP tools are available"),
                        "vanilla says so explicitly"),
                () -> assertTrue(prompt.contains("Record each change in the JSON output"),
                        "vanilla tells the agent how to log its manual edits"));
    }

    @Test
    void gradleOnlyVariantAdvertisesThreeTools() {
        final String prompt = PromptBuilder.build("Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), RunVariant.MCP_GRADLE_ONLY);
        assertAll(
                () -> assertTrue(prompt.contains("verify_build(module)"),
                        "gradle-only offers verify_build"),
                () -> assertTrue(prompt.contains("run_tests(module, testClass?)"),
                        "gradle-only offers run_tests"),
                () -> assertTrue(prompt.contains("format(module)"),
                        "gradle-only offers format"),
                () -> assertTrue(prompt.contains("DO NOT call `extract_method`"),
                        "gradle-only explicitly forbids extract_method"),
                () -> assertFalse(prompt.contains("extract_method(file"),
                        "the recipe signature is not listed"));
    }

    @Test
    void recipesVariantAdvertisesAllFourTools() {
        final String prompt = PromptBuilder.build("Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), RunVariant.MCP_RECIPES);
        assertAll(
                () -> assertTrue(prompt.contains("extract_method(file, startLine, endLine, newMethodName)"),
                        "recipes variant lists the full extract_method signature"),
                () -> assertTrue(prompt.contains("verify_build(module)")),
                () -> assertTrue(prompt.contains("run_tests(module, testClass?)")),
                () -> assertTrue(prompt.contains("format(module)")),
                () -> assertTrue(prompt.contains("Prefer the recipe tool"),
                        "recipes variant nudges toward the tool over manual edit"));
    }

    @Test
    void everyVariantIncludesFilePointerAndFindingsAndSchema() {
        for (final RunVariant variant : RunVariant.values()) {
            final String prompt = PromptBuilder.build("Bar.java",
                    List.of(new Suggestion(HeuristicCode.G22, 5, "not final")), variant);
            assertAll(
                    () -> assertTrue(prompt.contains("Target file (relative to project root): Bar.java"),
                            "file pointer present for " + variant),
                    () -> assertTrue(prompt.contains("Read the file with your Read tool"),
                            "Read instruction present for " + variant),
                    () -> assertTrue(prompt.contains("G22 at L5: not final"),
                            "suggestions present for " + variant),
                    () -> assertTrue(prompt.contains("\"actions\""),
                            "JSON schema present for " + variant));
        }
    }
}
