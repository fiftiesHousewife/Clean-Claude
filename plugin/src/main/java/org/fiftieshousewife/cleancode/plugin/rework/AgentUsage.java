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
        long durationMs,
        int numTurns,
        double totalCostUsd) {

    public int totalInputTokens() {
        return inputTokens + cacheCreationInputTokens + cacheReadInputTokens;
    }

    public double cacheHitRate() {
        final long total = (long) cacheCreationInputTokens + cacheReadInputTokens;
        return total == 0 ? 0.0 : (double) cacheReadInputTokens / total;
    }
}
