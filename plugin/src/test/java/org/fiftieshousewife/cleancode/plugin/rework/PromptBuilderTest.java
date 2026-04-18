package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void withRecipeToolsAdvertisesMcpExtractMethodAndWorkflow() {
        final String prompt = PromptBuilder.build("Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), true);
        assertAll(
                () -> assertTrue(prompt.contains("extract_method(file, startLine, endLine, newMethodName)"),
                        "MCP tool signature is shown when tools are enabled"),
                () -> assertTrue(prompt.contains("cleancode-refactoring` MCP server"),
                        "the server name matches what `claude mcp add` registers"),
                () -> assertTrue(prompt.contains("If a tool returns an error"),
                        "the workflow note tells the agent to log rejections instead of forcing"));
    }

    @Test
    void withoutRecipeToolsTellsAgentToEditByHand() {
        final String prompt = PromptBuilder.build("Foo.java",
                List.of(new Suggestion(HeuristicCode.G30, 10, "long")), false);
        assertAll(
                () -> assertFalse(prompt.contains("extract_method("),
                        "no MCP tool advertisement when tools are explicitly excluded"),
                () -> assertTrue(prompt.contains("manually with your Edit"),
                        "fallback instruction present"),
                () -> assertTrue(prompt.contains("\"Edit\" as the recipe name"),
                        "the schema guidance matches the manual-edit mode"));
    }

    @Test
    void bothModesIncludeFilePointerAndSuggestionsAndSchemaButNotContents() {
        final String withTools = PromptBuilder.build("Bar.java",
                List.of(new Suggestion(HeuristicCode.G22, 5, "not final")), true);
        final String withoutTools = PromptBuilder.build("Bar.java",
                List.of(new Suggestion(HeuristicCode.G22, 5, "not final")), false);
        for (final String prompt : List.of(withTools, withoutTools)) {
            assertAll(
                    () -> assertTrue(prompt.contains("Target file (relative to project root): Bar.java"),
                            "file pointer present so the agent knows what to Read"),
                    () -> assertTrue(prompt.contains("Read the file with your Read tool"),
                            "explicit instruction to use the Read tool"),
                    () -> assertTrue(prompt.contains("G22 at L5: not final"),
                            "suggestions appear in both modes"),
                    () -> assertTrue(prompt.contains("\"actions\""),
                            "JSON schema is present in both modes"));
        }
    }
}
