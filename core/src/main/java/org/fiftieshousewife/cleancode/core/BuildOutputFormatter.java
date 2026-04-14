package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BuildOutputFormatter {

    private static final String HEADER =
            "═══════════════════════════════════════════════════════════════";
    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────";

    private BuildOutputFormatter() {}

    public static String format(AggregatedReport report) {
        final StringBuilder out = new StringBuilder();
        final List<Finding> findings = report.findings();

        out.append('\n').append(HEADER).append('\n');
        out.append("  CLEAN CODE ANALYSIS  —  ").append(report.projectName()).append('\n');
        out.append(HEADER).append('\n');

        if (findings.isEmpty()) {
            out.append("\n  No violations found.\n");
            out.append('\n').append(HEADER).append('\n');
            return out.toString();
        }

        appendSeveritySummary(out, report);
        appendFindingsByCode(out, findings);
        appendToolSummary(out, findings);
        appendFooter(out, findings);

        return out.toString();
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

        out.append('\n').append(DIVIDER).append('\n');

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> {
                    final HeuristicCode code = entry.getKey();
                    final List<Finding> group = entry.getValue();
                    out.append("\n  ").append(code.name()).append(" (").append(group.size()).append(")\n");
                    group.stream()
                            .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                            .forEach(f -> appendFinding(out, f));
                });
    }

    private static void appendFinding(StringBuilder out, Finding finding) {
        final String location = formatLocation(finding);
        out.append("    ").append(severityIcon(finding.severity()));
        out.append(" ").append(location);
        out.append("  ").append(finding.message()).append('\n');
    }

    private static String formatLocation(Finding finding) {
        if (finding.sourceFile() == null) {
            return "(project)";
        }
        final String file = shortenPath(finding.sourceFile());
        if (finding.startLine() > 0) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    private static String shortenPath(String path) {
        final int srcIdx = path.indexOf("src/main/java/");
        if (srcIdx >= 0) {
            return path.substring(srcIdx + "src/main/java/".length());
        }
        return path;
    }

    private static String severityIcon(Severity severity) {
        return switch (severity) {
            case ERROR -> "!!";
            case WARNING -> " !";
            case INFO -> "  ";
        };
    }

    private static void appendToolSummary(StringBuilder out, List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));

        out.append('\n').append(DIVIDER).append('\n');
        out.append("\n  Sources:\n");

        byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                        out.append("    ").append(entry.getKey())
                                .append(": ").append(entry.getValue()).append('\n'));
    }

    private static void appendFooter(StringBuilder out, List<Finding> findings) {
        out.append('\n').append(HEADER).append('\n');
        out.append("  ").append(findings.size()).append(" findings");
        out.append("  —  Run ./gradlew cleanCodeExplain --finding=<code> for guidance\n");
        out.append(HEADER).append('\n');
    }
}
