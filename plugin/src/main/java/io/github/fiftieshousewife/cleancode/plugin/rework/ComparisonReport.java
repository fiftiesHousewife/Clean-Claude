package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;
import java.util.Optional;

/**
 * Renders the side-by-side markdown that {@link ReworkCompareTask}
 * produces after N paired rework runs. Three tables per report: a cost
 * table covering every token category the agent runtime reports plus
 * derived values (total input, cache hit rate, cost per action), a
 * findings table (baseline / fixed / introduced / final) that shows
 * what each variant actually moved on the target files, and per-variant
 * diff + commit-message sections underneath.
 */
public final class ComparisonReport {

    public record VariantRun(RunVariant variant, ReworkReport report, String diff, FindingsSnapshot findings) {}

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
        appendFindingsTable(body, runs);
        runs.forEach(run -> appendVariantSection(body, run));
        return body.toString();
    }

    private static void appendCostTable(final StringBuilder body, final List<VariantRun> runs) {
        body.append("## Cost\n\n");
        body.append("| |").append(header(runs)).append('\n');
        body.append("|---|").append(alignment(runs)).append('\n');
        appendRow(body, runs, "input tokens",
                u -> String.valueOf(u.inputTokens()));
        appendRow(body, runs, "cache creation",
                u -> String.valueOf(u.cacheCreationInputTokens()));
        appendRow(body, runs, "cache read",
                u -> String.valueOf(u.cacheReadInputTokens()));
        appendRow(body, runs, "total input",
                u -> String.valueOf(u.totalInputTokens()));
        appendRow(body, runs, "output tokens",
                u -> String.valueOf(u.outputTokens()));
        appendRow(body, runs, "cache hit rate",
                u -> String.format("%.1f%%", u.cacheHitRate() * 100));
        appendRow(body, runs, "turns",
                u -> String.valueOf(u.numTurns()));
        appendRow(body, runs, "duration (s)",
                u -> String.format("%.1f", u.durationMs() / 1000.0));
        appendRow(body, runs, "cost (USD)",
                u -> String.format("%.4f", u.totalCostUsd()));
        appendCountRow(body, runs, "actions", r -> r.actionsTaken().size());
        appendCountRow(body, runs, "rejected", r -> r.rejected().size());
        appendCostPerActionRow(body, runs);
        body.append('\n');
    }

    private static void appendFindingsTable(final StringBuilder body, final List<VariantRun> runs) {
        body.append("## Findings\n\n");
        body.append("| |").append(header(runs)).append('\n');
        body.append("|---|").append(alignment(runs)).append('\n');
        appendFindingsRow(body, runs, "baseline", FindingsSnapshot::baseline);
        appendFindingsRow(body, runs, "fixed", FindingsSnapshot::fixed);
        appendFindingsRow(body, runs, "introduced", FindingsSnapshot::introduced);
        appendFindingsRow(body, runs, "final", FindingsSnapshot::finalCount);
        body.append('\n');
    }

    private static void appendCostPerActionRow(final StringBuilder body, final List<VariantRun> runs) {
        body.append("| cost per action |");
        runs.forEach(run -> {
            final int actions = run.report().actionsTaken().size();
            final Optional<AgentUsage> usage = run.report().usage();
            final String cell = usage.filter(u -> actions > 0)
                    .map(u -> String.format("%.4f", u.totalCostUsd() / actions))
                    .orElse("—");
            body.append(' ').append(cell).append(" |");
        });
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

    private static void appendRow(final StringBuilder body, final List<VariantRun> runs,
                                  final String label, final UsageField accessor) {
        body.append("| ").append(label).append(" |");
        runs.forEach(run -> body.append(' ')
                .append(run.report().usage().map(accessor::apply).orElse("—")).append(" |"));
        body.append('\n');
    }

    @FunctionalInterface
    private interface ReportField {
        int apply(ReworkReport report);
    }

    private static void appendCountRow(final StringBuilder body, final List<VariantRun> runs,
                                       final String label, final ReportField accessor) {
        body.append("| ").append(label).append(" |");
        runs.forEach(run -> body.append(' ').append(accessor.apply(run.report())).append(" |"));
        body.append('\n');
    }

    @FunctionalInterface
    private interface FindingsField {
        int apply(FindingsSnapshot snapshot);
    }

    private static void appendFindingsRow(final StringBuilder body, final List<VariantRun> runs,
                                          final String label, final FindingsField accessor) {
        body.append("| ").append(label).append(" |");
        runs.forEach(run -> body.append(' ').append(accessor.apply(run.findings())).append(" |"));
        body.append('\n');
    }

    private static String label(final RunVariant variant) {
        return switch (variant) {
            case VANILLA -> "vanilla";
            case MCP_GRADLE_ONLY -> "mcp gradle only";
            case MCP_RECIPES -> "mcp + recipes";
            case HARNESS_RECIPES_THEN_AGENT -> "harness + agent";
            case RECIPES_ONLY -> "recipes only";
        };
    }
}
