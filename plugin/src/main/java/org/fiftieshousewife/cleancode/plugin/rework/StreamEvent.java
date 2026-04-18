package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.Optional;

/**
 * One line of output from {@code claude -p --output-format stream-json}
 * classified into the cases the rework harness cares about. Kept as a
 * sealed interface so {@link StreamingAgentReader} can pattern-match
 * on known event types and ignore the rest.
 */
public sealed interface StreamEvent {

    record AssistantText(String text) implements StreamEvent {}

    record ToolUse(String name) implements StreamEvent {}

    record ToolResult() implements StreamEvent {}

    record Result(String text, Optional<AgentUsage> usage) implements StreamEvent {}

    record Ignored() implements StreamEvent {}
}
