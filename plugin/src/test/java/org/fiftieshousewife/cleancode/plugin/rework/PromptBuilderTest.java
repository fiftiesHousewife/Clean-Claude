package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private static List<FileTarget> oneTarget() {
        return List.of(new FileTarget(Path.of("/abs/Foo.java"), "Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long"))));
    }

    @Test
    void vanillaVariantMentionsNoMcpToolsAndFallsBackToManualEdit() {
        final String prompt = PromptBuilder.build(oneTarget(), RunVariant.VANILLA);
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
        final String prompt = PromptBuilder.build(oneTarget(), RunVariant.MCP_GRADLE_ONLY);
        assertAll(
                () -> assertTrue(prompt.contains("verify_build(module)")),
                () -> assertTrue(prompt.contains("run_tests(module, testClass?)")),
                () -> assertTrue(prompt.contains("format(module)")),
                () -> assertTrue(prompt.contains("DO NOT call `extract_method`"),
                        "gradle-only explicitly forbids extract_method"),
                () -> assertFalse(prompt.contains("extract_method(file"),
                        "the recipe signature is not listed"));
    }

    @Test
    void recipesVariantAdvertisesAllFourTools() {
        final String prompt = PromptBuilder.build(oneTarget(), RunVariant.MCP_RECIPES);
        assertAll(
                () -> assertTrue(prompt.contains("extract_method(file, startLine, endLine, newMethodName)"),
                        "recipes variant lists the full extract_method signature"),
                () -> assertTrue(prompt.contains("verify_build(module)")),
                () -> assertTrue(prompt.contains("run_tests(module, testClass?)")),
                () -> assertTrue(prompt.contains("format(module)")),
                () -> assertTrue(prompt.contains("USE extract_method when ALL of these hold"),
                        "recipes variant spells out preconditions explicitly"),
                () -> assertTrue(prompt.contains("DO NOT use extract_method when"),
                        "recipes variant spells out anti-patterns"));
    }

    @Test
    void batchSessionListsEveryTargetAndItsFindingsSeparately() {
        final List<FileTarget> targets = List.of(
                new FileTarget(Path.of("/abs/A.java"), "a/A.java",
                        List.of(new Suggestion(HeuristicCode.G30, 10, "long method"))),
                new FileTarget(Path.of("/abs/B.java"), "b/B.java",
                        List.of(new Suggestion(HeuristicCode.G18, 5, "static candidate"))));

        final String prompt = PromptBuilder.build(targets, RunVariant.MCP_RECIPES);

        assertAll(
                () -> assertTrue(prompt.contains("reworking 2 Java files"),
                        "header counts the batch"),
                () -> assertTrue(prompt.contains("- a/A.java") && prompt.contains("- b/B.java"),
                        "each target appears in the file list"),
                () -> assertTrue(prompt.contains("### a/A.java") && prompt.contains("### b/B.java"),
                        "findings are grouped per file"),
                () -> assertTrue(prompt.contains("G30 at L10: long method")),
                () -> assertTrue(prompt.contains("G18 at L5: static candidate")));
    }
}
