package org.fiftieshousewife.cleancode.plugin.rework;

/**
 * Token accounting captured from the {@code claude -p --output-format json}
 * envelope. Surfaced in {@link ReworkReport} and in the commit-message body
 * so paired rework runs (with/without recipe tools) can be compared on
 * cost, not just quality.
 */
public record AgentUsage(
        int inputTokens,
        int outputTokens,
        int cacheCreationInputTokens,
        int cacheReadInputTokens,
        double totalCostUsd) {}
