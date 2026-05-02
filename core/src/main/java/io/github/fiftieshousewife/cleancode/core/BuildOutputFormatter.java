package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BuildOutputFormatter {

    private static final String HEADER =
            "═══════════════════════════════════════════════════════════════════════════";
    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────────────────";
    private static final int WRAP_WIDTH = 72;

    private BuildOutputFormatter() {}

    public static String format(AggregatedReport report) {
        return format(report, Map.of(), List.of());
    }

    public static String format(AggregatedReport report,
                                Map<HeuristicCode, BaselineManager.Delta> deltas) {
        return format(report, deltas, List.of());
    }

    public static String format(AggregatedReport report,
                                Map<HeuristicCode, BaselineManager.Delta> deltas,
                                List<SourceState> sourceStates) {
        final StringBuilder out = new StringBuilder();
        final List<Finding> findings = report.findings();

        out.append('\n').append(HEADER).append('\n');
        out.append("  CLEAN CODE ANALYSIS  —  ").append(report.projectName()).append('\n');
        out.append(HEADER).append('\n');

        if (findings.isEmpty()) {
            out.append("\n  No violations found. The code is clean.\n");
            appendSourceStateSummary(out, sourceStates);
            appendOptionalRulesSummary(out);
            out.append('\n').append(HEADER).append('\n');
            return out.toString();
        }

        appendSeveritySummary(out, report);

        if (!deltas.isEmpty()) {
            appendBaselineDelta(out, deltas);
        }

        appendFindingsByCode(out, findings);
        appendToolSummary(out, findings, sourceStates);
        appendOptionalRulesSummary(out);
        appendFooter(out, findings);

        return out.toString();
    }

    private static void appendBaselineDelta(StringBuilder out,
                                            Map<HeuristicCode, BaselineManager.Delta> deltas) {
        final int newViolations = deltas.values().stream()
                .mapToInt(d -> Math.max(0, d.change()))
                .sum();
        final int fixedViolations = deltas.values().stream()
                .mapToInt(d -> Math.max(0, -d.change()))
                .sum();

        out.append("  vs baseline: ");
        if (newViolations > 0) {
            out.append("+").append(newViolations).append(" new");
        }
        if (newViolations > 0 && fixedViolations > 0) {
            out.append("  ·  ");
        }
        if (fixedViolations > 0) {
            out.append("-").append(fixedViolations).append(" fixed");
        }
        if (newViolations == 0 && fixedViolations == 0) {
            out.append("no change");
        }
        out.append('\n');
    }

    private static void appendSeveritySummary(StringBuilder out, AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        final int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        final int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        final int info = bySeverity.getOrDefault(Severity.INFO, List.of()).size();

        out.append('\n');
        out.append("  ").append(errors).append(" errors");
        out.append("  ·  ").append(warnings).append(" warnings");
        out.append("  ·  ").append(info).append(" info\n");
    }

    private static void appendFindingsByCode(StringBuilder out, List<Finding> findings) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> appendCodeGroup(out, entry.getKey(), entry.getValue()));
    }

    private static void appendCodeGroup(StringBuilder out, HeuristicCode code, List<Finding> group) {
        out.append('\n').append(DIVIDER).append('\n');

        final String name = HeuristicDescriptions.name(code);
        out.append("  ").append(code.name()).append(": ").append(name);
        out.append(" (").append(group.size()).append(")\n");

        final String summary = HeuristicDescriptions.summary(code);
        if (summary != null) {
            out.append("  ").append(summary).append('\n');
        }

        final String reference = HeuristicDescriptions.reference(code);
        if (reference != null) {
            out.append("  ").append(reference).append('\n');
        }

        final String guidance = HeuristicDescriptions.guidance(code);
        if (guidance != null) {
            out.append('\n');
            appendWrapped(out, guidance, "  ", WRAP_WIDTH);
        }

        out.append('\n');
        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFinding(out, f));
    }

    private static void appendFinding(StringBuilder out, Finding finding) {
        final String location = formatLocation(finding);
        out.append("    ").append(severityIcon(finding.severity()));
        out.append(" ").append(location);
        out.append("  ").append(finding.message()).append('\n');
    }

    private static void appendWrapped(StringBuilder out, String text, String indent, int width) {
        final String[] words = text.split(" ");
        final StringBuilder line = new StringBuilder(indent);
        for (final String word : words) {
            if (line.length() + word.length() + 1 > width && line.length() > indent.length()) {
                out.append(line).append('\n');
                line.setLength(0);
                line.append(indent);
            }
            if (line.length() > indent.length()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > indent.length()) {
            out.append(line).append('\n');
        }
    }

    private static String formatLocation(Finding finding) {
        if (finding.sourceFile() == null) {
            return "(project)";
        }
        final String file = finding.sourceFile();
        if (finding.startLine() > 0) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    private static String severityIcon(Severity severity) {
        return switch (severity) {
            case ERROR -> "!!";
            case WARNING -> " !";
            case INFO -> "  ";
        };
    }

    private static void appendToolSummary(StringBuilder out, List<Finding> findings, List<SourceState> sourceStates) {
        out.append('\n').append(DIVIDER).append('\n');
        out.append("  Sources:\n");

        if (sourceStates.isEmpty()) {
            final Map<String, Long> byTool = findings.stream()
                    .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));
            byTool.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry ->
                            out.append("    ").append(entry.getKey())
                                    .append(": ").append(entry.getValue()).append('\n'));
            return;
        }

        sourceStates.stream()
                .sorted(Comparator.comparingInt((SourceState s) -> -s.findingCount())
                        .thenComparing(SourceState::displayName))
                .forEach(state -> out.append("    ").append(formatSourceState(state)).append('\n'));
    }

    private static String formatSourceState(final SourceState state) {
        return switch (state.status()) {
            case PRODUCED_FINDINGS -> state.displayName() + ": " + state.findingCount();
            case RAN_NO_FINDINGS -> state.displayName() + ": (ran, no findings)";
            case NOT_AVAILABLE -> state.displayName() + ": (not available — report not found)";
        };
    }

    private static void appendSourceStateSummary(StringBuilder out, List<SourceState> sourceStates) {
        if (sourceStates.isEmpty()) {
            return;
        }
        out.append('\n').append(DIVIDER).append('\n');
        out.append("  Sources:\n");
        sourceStates.forEach(state -> out.append("    ").append(formatSourceState(state)).append('\n'));
    }

    private static void appendOptionalRulesSummary(StringBuilder out) {
        if (OptionalRules.defaults().isEmpty()) {
            return;
        }
        out.append('\n').append(DIVIDER).append('\n');
        out.append("  Optional rules (disabled by default — opt in via cleanCode.enabledOptionalRules):\n");
        OptionalRules.defaults().values().forEach(rule -> {
            out.append("    ").append(rule.key()).append("  [").append(rule.code()).append("]\n");
            out.append("      ").append(rule.summary()).append('\n');
        });
    }

    private static void appendFooter(StringBuilder out, List<Finding> findings) {
        out.append('\n').append(HEADER).append('\n');
        out.append("  ").append(findings.size()).append(" findings");
        out.append("  —  ./gradlew cleanCodeExplain --finding=<code>\n");
        out.append(HEADER).append('\n');
    }
}
