package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class FindingsByCodeFormatter {

    private static final String DIVIDER =
            "───────────────────────────────────────────────────────────────────────────";
    private static final int WRAP_WIDTH = 72;

    private FindingsByCodeFormatter() {}

    static void append(final StringBuilder out, final List<Finding> findings) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> appendCodeGroup(out, entry.getKey(), entry.getValue()));
    }

    private static void appendCodeGroup(final StringBuilder out,
                                        final HeuristicCode code,
                                        final List<Finding> group) {
        out.append('\n').append(DIVIDER).append('\n');

        final String name = HeuristicDescriptions.name(code);
        out.append("  ").append(code.name()).append(": ").append(name);
        out.append(" (").append(group.size()).append(")\n");

        appendIfPresent(out, HeuristicDescriptions.summary(code));
        appendIfPresent(out, HeuristicDescriptions.reference(code));

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

    private static void appendIfPresent(final StringBuilder out, final String text) {
        if (text != null) {
            out.append("  ").append(text).append('\n');
        }
    }

    private static void appendFinding(final StringBuilder out, final Finding finding) {
        final String location = formatLocation(finding);
        out.append("    ").append(severityIcon(finding.severity()));
        out.append(" ").append(location);
        out.append("  ").append(finding.message()).append('\n');
    }

    private static void appendWrapped(final StringBuilder out,
                                      final String text,
                                      final String indent,
                                      final int width) {
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

    private static String formatLocation(final Finding finding) {
        if (finding.sourceFile() == null) {
            return "(project)";
        }
        final String file = finding.sourceFile();
        if (finding.startLine() > 0) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    private static String severityIcon(final Severity severity) {
        return switch (severity) {
            case ERROR -> "!!";
            case WARNING -> " !";
            case INFO -> "  ";
        };
    }
}
