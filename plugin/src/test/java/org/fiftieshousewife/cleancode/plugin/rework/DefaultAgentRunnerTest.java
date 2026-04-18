package org.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAgentRunnerTest {

    @Test
    void parsesClaudeJsonEnvelopeIntoTextAndUsage() {
        final String envelope = """
                {"type":"result","subtype":"success","is_error":false,
                 "session_id":"abc","num_turns":1,
                 "result":"the agent's textual response\\n{\\"actions\\":[]}",
                 "total_cost_usd":0.0123,
                 "usage":{"input_tokens":1500,"output_tokens":400,
                          "cache_creation_input_tokens":0,
                          "cache_read_input_tokens":200},
                 "duration_ms":456}
                """;

        final AgentResult result = DefaultAgentRunner.parseEnvelope(envelope);

        assertAll(
                () -> assertTrue(result.text().contains("the agent's textual response"),
                        "the inner `result` field is surfaced as the agent's text"),
                () -> assertTrue(result.text().contains("\"actions\":[]"),
                        "embedded JSON inside `result` survives unescaping"),
                () -> assertTrue(result.usage().isPresent(),
                        "usage envelope turns into an AgentUsage"),
                () -> assertEquals(1500, result.usage().get().inputTokens()),
                () -> assertEquals(400, result.usage().get().outputTokens()),
                () -> assertEquals(200, result.usage().get().cacheReadInputTokens()),
                () -> assertEquals(0.0123, result.usage().get().totalCostUsd(), 0.0001));
    }

    @Test
    void missingUsageFieldStillYieldsText() {
        final String envelope = "{\"type\":\"result\",\"result\":\"some reply\"}";

        final AgentResult result = DefaultAgentRunner.parseEnvelope(envelope);

        assertAll(
                () -> assertEquals("some reply", result.text()),
                () -> assertTrue(result.usage().isEmpty(),
                        "older claude versions without usage still return a usable result"));
    }

    @Test
    void nonJsonStdoutFallsBackToTextOnly() {
        final String stdout = "claude: command not found";

        final AgentResult result = DefaultAgentRunner.parseEnvelope(stdout);

        assertAll(
                () -> assertEquals(stdout, result.text(),
                        "when the envelope is not JSON, surface the raw output so the caller can log it"),
                () -> assertTrue(result.usage().isEmpty()));
    }
}
