package org.fiftieshousewife.cleancode.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

final class BriefIndexMarkdown {

    private BriefIndexMarkdown() {}

    static String render(final String projectName, final Map<String, List<Finding>> byFile,
                         final Function<String, String> briefFileNameFor) {
        final StringBuilder sb = new StringBuilder()
                .append("# Fix briefs for ").append(projectName).append('\n')
                .append('\n')
                .append("One brief per file. Each brief lists every finding on that file and points at the "
                        + "relevant skill. Designed to be handed to a single agent so two agents never edit the same "
                        + "file.\n")
                .append('\n')
                .append("| File | Findings | Brief |\n")
                .append("|---|---:|---|\n");
        byFile.entrySet().stream()
                .sorted(Entry.<String, List<Finding>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .forEach(e -> appendRow(sb, e, briefFileNameFor));
        return sb.toString();
    }

    private static void appendRow(final StringBuilder sb, final Entry<String, List<Finding>> entry,
                                  final Function<String, String> briefFileNameFor) {
        final String briefFileName = briefFileNameFor.apply(entry.getKey());
        sb.append("| ").append(entry.getKey())
                .append(" | ").append(entry.getValue().size())
                .append(" | [").append(briefFileName).append("](")
                .append(briefFileName).append(") |\n");
    }
}
