package org.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentResponseParserTest {

    @Test
    void parsesRawJsonObject() {
        final String json = """
                {"actions":[{"recipe":"ExtractMethodRecipe",
                             "options":{"file":"Foo.java","startLine":42,"endLine":67,
                                        "newMethodName":"computeThing"},
                             "why":"groups the metrics table build"}],
                 "rejected":[{"recipe":"ExtractMethodRecipe",
                              "options":{"startLine":70,"endLine":90},
                              "why":"range contains throw"}]}
                """;

        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(json);

        final List<AgentAction> actions = parsed.actions();
        final List<AgentRejection> rejected = parsed.rejected();
        assertAll(
                () -> assertEquals(1, actions.size()),
                () -> assertEquals("ExtractMethodRecipe", actions.getFirst().recipe()),
                () -> assertEquals("computeThing", actions.getFirst().options().get("newMethodName")),
                () -> assertEquals("groups the metrics table build", actions.getFirst().why()),
                () -> assertEquals(1, rejected.size()),
                () -> assertEquals("range contains throw", rejected.getFirst().why()));
    }

    @Test
    void parsesJsonInsideFencedBlock() {
        final String output = """
                Here's what I did:
                ```json
                {"actions":[{"recipe":"MoveMethodRecipe",
                             "options":{"file":"Utils.java","methodName":"doubleIt",
                                        "targetFqn":"com.example.Helpers"},
                             "why":"Utils has only this one util"}],
                 "rejected":[]}
                ```
                That's all.
                """;

        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(output);

        assertAll(
                () -> assertEquals(1, parsed.actions().size()),
                () -> assertTrue(parsed.rejected().isEmpty()),
                () -> assertEquals("MoveMethodRecipe", parsed.actions().getFirst().recipe()));
    }

    @Test
    void returnsEmptyListsWhenNoJsonPresent() {
        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(
                "I wasn't able to do anything useful here.");

        assertAll(
                () -> assertTrue(parsed.actions().isEmpty()),
                () -> assertTrue(parsed.rejected().isEmpty()),
                () -> assertTrue(parsed.error().isPresent(),
                        "missing JSON surfaces as an error the commit message can mention"));
    }

    @Test
    void missingArraysBecomeEmpty() {
        final String json = "{\"actions\":[],\"rejected\":[]}";

        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(json);

        assertAll(
                () -> assertTrue(parsed.actions().isEmpty()),
                () -> assertTrue(parsed.rejected().isEmpty()),
                () -> assertTrue(parsed.error().isEmpty(),
                        "well-formed empty result is not an error"));
    }
}
