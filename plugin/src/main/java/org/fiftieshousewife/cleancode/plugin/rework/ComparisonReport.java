package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.Optional;

/**
 * Renders the side-by-side markdown that {@link ReworkCompareTask}
 * produces after two paired rework runs (with vs without recipe
 * tools). Keeps formatting decisions in one place so the output's
 * shape can be tweaked without touching task orchestration.
 */
public final class ComparisonReport {

    private ComparisonReport() {}

    public static String format(final ReworkReport withTools, final String diffWithTools,
                                final ReworkReport withoutTools, final String diffWithoutTools) {
        return """
                # Rework comparison

                Target file: %s

                ## Cost

                | | with recipe tools | without recipe tools |
                |---|---:|---:|
                | input tokens | %s | %s |
                | output tokens | %s | %s |
                | cache read | %s | %s |
                | cost (USD) | %s | %s |
                | actions | %d | %d |
                | rejected | %d | %d |

                ## With recipe tools — commit message body

                %s

                ## With recipe tools — diff

                ```diff
                %s
                ```

                ## Without recipe tools — commit message body

                %s

                ## Without recipe tools — diff

                ```diff
                %s
                ```
                """.formatted(
                        withTools.file(),
                        usageField(withTools.usage(), AgentUsage::inputTokens),
                        usageField(withoutTools.usage(), AgentUsage::inputTokens),
                        usageField(withTools.usage(), AgentUsage::outputTokens),
                        usageField(withoutTools.usage(), AgentUsage::outputTokens),
                        usageField(withTools.usage(), AgentUsage::cacheReadInputTokens),
                        usageField(withoutTools.usage(), AgentUsage::cacheReadInputTokens),
                        usageDollars(withTools.usage()),
                        usageDollars(withoutTools.usage()),
                        withTools.actionsTaken().size(),
                        withoutTools.actionsTaken().size(),
                        withTools.rejected().size(),
                        withoutTools.rejected().size(),
                        bodyOrPlaceholder(withTools),
                        diffWithTools.isBlank() ? "(no changes)" : diffWithTools,
                        bodyOrPlaceholder(withoutTools),
                        diffWithoutTools.isBlank() ? "(no changes)" : diffWithoutTools);
    }

    @FunctionalInterface
    private interface UsageField {
        int apply(AgentUsage usage);
    }

    private static String usageField(final Optional<AgentUsage> usage, final UsageField accessor) {
        return usage.map(u -> Integer.toString(accessor.apply(u))).orElse("—");
    }

    private static String usageDollars(final Optional<AgentUsage> usage) {
        return usage.map(u -> String.format("%.4f", u.totalCostUsd())).orElse("—");
    }

    private static String bodyOrPlaceholder(final ReworkReport report) {
        return report.commitMessageBody().isBlank()
                ? "(empty — agent reported no actions)"
                : report.commitMessageBody();
    }
}
