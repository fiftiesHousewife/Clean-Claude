package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void withRecipeToolsListsGradleInvocationsAndRecipeWorkflow() {
        final String prompt = PromptBuilder.build("Foo.java", "class Foo {}",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), true);
        assertAll(
                () -> assertTrue(prompt.contains(":refactoring:extractMethod"),
                        "extractMethod invocation is shown when tools are enabled"),
                () -> assertTrue(prompt.contains(":refactoring:moveMethod")),
                () -> assertTrue(prompt.contains("rejects, record the rejection"),
                        "the workflow note tells the agent to log rejections instead of forcing"));
    }

    @Test
    void withoutRecipeToolsTellsAgentToEditByHand() {
        final String prompt = PromptBuilder.build("Foo.java", "class Foo {}",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), false);
        assertAll(
                () -> assertFalse(prompt.contains(":refactoring:extractMethod"),
                        "no recipe CLI when the tools are explicitly excluded"),
                () -> assertTrue(prompt.contains("manually with your Edit"),
                        "fallback instruction present"),
                () -> assertTrue(prompt.contains("\"Edit\" as the recipe name"),
                        "the schema guidance matches the manual-edit mode"));
    }

    @Test
    void bothModesIncludeFileContentsAndSuggestionsAndSchema() {
        final String withTools = PromptBuilder.build("Bar.java", "class Bar {}",
                List.of(new Suggestion(HeuristicCode.G22, 5, "not final")), true);
        final String withoutTools = PromptBuilder.build("Bar.java", "class Bar {}",
                List.of(new Suggestion(HeuristicCode.G22, 5, "not final")), false);
        for (final String prompt : List.of(withTools, withoutTools)) {
            assertAll(
                    () -> assertTrue(prompt.contains("class Bar"),
                            "file contents appear in both modes"),
                    () -> assertTrue(prompt.contains("G22 at L5: not final"),
                            "suggestions appear in both modes"),
                    () -> assertTrue(prompt.contains("\"actions\""),
                            "JSON schema is present in both modes"));
        }
    }
}
