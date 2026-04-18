package org.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAgentRunnerTest {

    @Test
    void parsesAssistantTextEventAsProgressSource() {
        final String line = """
                {"type":"assistant","message":{"content":[{"type":"text","text":"I'll extract the guard block"}]}}""";

        final StreamEvent event = StreamJsonEventParser.parseLine(line);

        final StreamEvent.AssistantText text = assertInstanceOf(StreamEvent.AssistantText.class, event);
        assertEquals("I'll extract the guard block", text.text());
    }

    @Test
    void parsesToolUseEventAsToolName() {
        final String line = "{\"type\":\"assistant\",\"message\":{\"content\":"
                + "[{\"type\":\"tool_use\",\"name\":\"Bash\",\"input\":{\"command\":\"ls\"}}]}}";

        final StreamEvent event = StreamJsonEventParser.parseLine(line);

        final StreamEvent.ToolUse toolUse = assertInstanceOf(StreamEvent.ToolUse.class, event);
        assertEquals("Bash", toolUse.name());
        assertEquals("ls", toolUse.inputSummary());
    }

    @Test
    void toolUseInputSummaryPrefersFilePathForReadEditWrite() {
        final String line = "{\"type\":\"assistant\",\"message\":{\"content\":"
                + "[{\"type\":\"tool_use\",\"name\":\"Edit\",\"input\":"
                + "{\"file_path\":\"/path/Foo.java\",\"old_string\":\"a\",\"new_string\":\"b\"}}]}}";

        final StreamEvent.ToolUse toolUse = assertInstanceOf(StreamEvent.ToolUse.class,
                StreamJsonEventParser.parseLine(line));

        assertEquals("/path/Foo.java", toolUse.inputSummary());
    }

    @Test
    void toolUseInputSummaryTruncatesLongBashCommands() {
        final String longCommand = "./gradlew :module:veryLongTaskName "
                + "--rerun-tasks --info --warning-mode all --stacktrace --debug-jvm "
                + "-Pmyflag=true -Panotherflag=false -Pyetanother=some-longer-value";
        final String line = "{\"type\":\"assistant\",\"message\":{\"content\":"
                + "[{\"type\":\"tool_use\",\"name\":\"Bash\",\"input\":{\"command\":\""
                + longCommand + "\"}}]}}";

        final StreamEvent.ToolUse toolUse = assertInstanceOf(StreamEvent.ToolUse.class,
                StreamJsonEventParser.parseLine(line));

        assertTrue(toolUse.inputSummary().endsWith("…"),
                "summaries beyond 100 chars end with an ellipsis so the stream stays one-line-per-event");
    }

    @Test
    void parsesToolResultEventAsToolResultSignal() {
        final String line = "{\"type\":\"user\",\"message\":{\"content\":"
                + "[{\"type\":\"tool_result\",\"content\":\"file contents\",\"tool_use_id\":\"...\"}]}}";

        final StreamEvent event = StreamJsonEventParser.parseLine(line);

        assertInstanceOf(StreamEvent.ToolResult.class, event);
    }

    @Test
    void parsesResultEventAsTextAndUsage() {
        final String line = """
                {"type":"result","subtype":"success","result":"the agent response with {\\"actions\\":[]}",
                 "total_cost_usd":0.0123,
                 "usage":{"input_tokens":1500,"output_tokens":400,
                          "cache_creation_input_tokens":0,
                          "cache_read_input_tokens":200}}""";

        final StreamEvent event = StreamJsonEventParser.parseLine(line);

        final StreamEvent.Result result = assertInstanceOf(StreamEvent.Result.class, event);
        assertAll(
                () -> assertTrue(result.text().contains("the agent response"),
                        "the inner result field surfaces as the agent text"),
                () -> assertTrue(result.text().contains("\"actions\":[]"),
                        "embedded JSON escaping roundtrips"),
                () -> assertTrue(result.usage().isPresent()),
                () -> assertEquals(1500, result.usage().get().inputTokens()),
                () -> assertEquals(400, result.usage().get().outputTokens()),
                () -> assertEquals(200, result.usage().get().cacheReadInputTokens()),
                () -> assertEquals(0.0123, result.usage().get().totalCostUsd(), 0.0001));
    }

    @Test
    void nonJsonLineIsIgnored() {
        assertInstanceOf(StreamEvent.Ignored.class, StreamJsonEventParser.parseLine("not json"));
    }

    @Test
    void unknownEventTypeIsIgnored() {
        final String line = "{\"type\":\"system\",\"subtype\":\"init\"}";
        assertInstanceOf(StreamEvent.Ignored.class, StreamJsonEventParser.parseLine(line));
    }

    @Test
    void blankLineIsIgnored() {
        assertInstanceOf(StreamEvent.Ignored.class, StreamJsonEventParser.parseLine(""));
    }
}
