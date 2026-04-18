package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;
import java.util.Optional;

/**
 * Renders the side-by-side markdown that {@link ReworkCompareTask}
 * produces after N paired rework runs (typically three:
 * {@link RunVariant#VANILLA}, {@link RunVariant#MCP_GRADLE_ONLY},
 * {@link RunVariant#MCP_RECIPES}). Keeps formatting decisions in one
 * place so the output's shape can be tweaked without touching task
 * orchestration.
 */
public final class ComparisonReport {

    public record VariantRun(RunVariant variant, ReworkReport report, String diff) {}

    private ComparisonReport() {}

    public static String format(final List<VariantRun> runs) {
        final StringBuilder body = new StringBuilder();
        body.append("# Rework comparison\n\n");
        if (!runs.isEmpty()) {
            body.append("Target files:\n");
            runs.getFirst().report().files().forEach(
                    file -> body.append("  - ").append(file).append('\n'));
            body.append('\n');
        }
        appendCostTable(body, runs);
        runs.forEach(run -> appendVariantSection(body, run));
        return body.toString();
    }

    private static void appendCostTable(final StringBuilder body, final List<VariantRun> runs) {
        body.append("## Cost\n\n");
        body.append("| |").append(header(runs)).append('\n');
        body.append("|---|").append(alignment(runs)).append('\n');
        body.append("| input tokens |").append(row(runs, u -> String.valueOf(u.inputTokens()))).append('\n');
        body.append("| output tokens |").append(row(runs, u -> String.valueOf(u.outputTokens()))).append('\n');
        body.append("| cache read |").append(row(runs, u -> String.valueOf(u.cacheReadInputTokens()))).append('\n');
        body.append("| cost (USD) |").append(row(runs, u -> String.format("%.4f", u.totalCostUsd()))).append('\n');
        body.append("| actions |")
                .append(countRow(runs, r -> r.actionsTaken().size())).append('\n');
        body.append("| rejected |")
                .append(countRow(runs, r -> r.rejected().size())).append('\n');
        body.append('\n');
    }

    private static void appendVariantSection(final StringBuilder body, final VariantRun run) {
        final String label = label(run.variant());
        body.append("## ").append(label).append(" — commit message body\n\n");
        body.append(run.report().commitMessageBody().isBlank()
                ? "(empty — agent reported no actions)"
                : run.report().commitMessageBody());
        body.append("\n\n## ").append(label).append(" — diff\n\n");
        body.append("```diff\n");
        body.append(run.diff().isBlank() ? "(no changes)" : run.diff());
        body.append("\n```\n\n");
    }

    private static String header(final List<VariantRun> runs) {
        final StringBuilder cells = new StringBuilder();
        runs.forEach(run -> cells.append(' ').append(label(run.variant())).append(" |"));
        return cells.toString();
    }

    private static String alignment(final List<VariantRun> runs) {
        return "---:|".repeat(runs.size());
    }

    @FunctionalInterface
    private interface UsageField {
        String apply(AgentUsage usage);
    }

    private static String row(final List<VariantRun> runs, final UsageField accessor) {
        final StringBuilder cells = new StringBuilder();
        runs.forEach(run -> {
            final Optional<AgentUsage> usage = run.report().usage();
            cells.append(' ').append(usage.map(accessor::apply).orElse("—")).append(" |");
        });
        return cells.toString();
    }

    @FunctionalInterface
    private interface ReportField {
        int apply(ReworkReport report);
    }

    private static String countRow(final List<VariantRun> runs, final ReportField accessor) {
        final StringBuilder cells = new StringBuilder();
        runs.forEach(run -> cells.append(' ').append(accessor.apply(run.report())).append(" |"));
        return cells.toString();
    }

    private static String label(final RunVariant variant) {
        return switch (variant) {
            case VANILLA -> "vanilla";
            case MCP_GRADLE_ONLY -> "mcp gradle only";
            case MCP_RECIPES -> "mcp + recipes";
        };
    }
}
