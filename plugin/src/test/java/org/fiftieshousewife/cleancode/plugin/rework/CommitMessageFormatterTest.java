package org.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitMessageFormatterTest {

    @Test
    void includesEachActionWithOptionsAndReason() {
        final AgentAction action = new AgentAction(
                "ExtractMethodRecipe",
                Map.of("file", "Foo.java", "startLine", 42, "endLine", 67, "newMethodName", "computeThing"),
                "groups the metrics table build");
        final String body = CommitMessageFormatter.format(List.of(action), List.of(), List.of(), Optional.empty());
        assertAll(
                () -> assertTrue(body.contains("## Actions"),
                        "actions get their own section so reviewers can scan"),
                () -> assertTrue(body.contains("ExtractMethodRecipe"),
                        "recipe name is on the action line"),
                () -> assertTrue(body.contains("newMethodName=computeThing"),
                        "option values are inline — no need to cross-reference"),
                () -> assertTrue(body.contains("groups the metrics table build"),
                        "agent's why is the load-bearing bit of the message"));
    }

    @Test
    void listsRejectionsSeparatelyWhenPresent() {
        final AgentRejection rejection = new AgentRejection(
                "ExtractMethodRecipe",
                Map.of("startLine", 70, "endLine", 90),
                "range contains throw");
        final String body = CommitMessageFormatter.format(List.of(), List.of(rejection), List.of(), Optional.empty());
        assertAll(
                () -> assertTrue(body.contains("## Not attempted")),
                () -> assertTrue(body.contains("range contains throw")));
    }

    @Test
    void omitsRejectionsSectionWhenEmpty() {
        final String body = CommitMessageFormatter.format(
                List.of(new AgentAction("NoopRecipe", Map.of(), "for coverage")),
                List.of(), List.of(), Optional.empty());
        assertFalse(body.contains("Not attempted"),
                "empty sections are noise in a commit message");
    }

    @Test
    void includesSuggestionsWhenNoAgentInvoked() {
        final String body = CommitMessageFormatter.format(List.of(), List.of(),
                List.of(new Suggestion(
                        org.fiftieshousewife.cleancode.annotations.HeuristicCode.G30,
                        42, "method too long")),
                Optional.empty());
        assertAll(
                () -> assertTrue(body.contains("## Suggestions")),
                () -> assertTrue(body.contains("G30")),
                () -> assertTrue(body.contains("method too long")));
    }

    @Test
    void includesUsageSectionWhenSupplied() {
        final AgentUsage usage = new AgentUsage(1500, 400, 200, 0, 12345L, 7, 0.0123);
        final String body = CommitMessageFormatter.format(
                List.of(new AgentAction("NoopRecipe", Map.of(), "for coverage")),
                List.of(), List.of(), Optional.of(usage));
        assertAll(
                () -> assertTrue(body.contains("## Agent usage")),
                () -> assertTrue(body.contains("input tokens : 1500")),
                () -> assertTrue(body.contains("output tokens: 400")),
                () -> assertTrue(body.contains("cost (USD)   : 0.0123")));
    }
}
