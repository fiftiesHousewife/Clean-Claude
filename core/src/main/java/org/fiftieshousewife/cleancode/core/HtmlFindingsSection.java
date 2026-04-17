package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class HtmlFindingsSection {

    private HtmlFindingsSection() {}

    static void appendByCode(final StringBuilder html, final List<Finding> findings,
                             final String repositoryUrl) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> appendCodeGroup(html, entry.getKey(), entry.getValue(), repositoryUrl));
    }

    static void appendToolSummary(final StringBuilder html, final List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));

        html.append("    <div class=\"tool-summary\">\n");
        html.append("      <h2>Sources</h2>\n");
        html.append("      <table>\n");
        html.append("        <tr><th>Tool</th><th>Findings</th></tr>\n");

        byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    html.append("        <tr><td>").append(HtmlEscaping.escape(entry.getKey()));
                    html.append("</td><td>").append(entry.getValue()).append("</td></tr>\n");
                });

        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private static void appendCodeGroup(final StringBuilder html, final HeuristicCode code,
                                        final List<Finding> group, final String repositoryUrl) {
        final String name = HeuristicDescriptions.name(code);
        final String reference = HeuristicDescriptions.reference(code);
        final String guidance = HeuristicDescriptions.guidance(code);

        html.append("    <details>\n");
        html.append("      <summary><span class=\"code-label\">").append(HtmlEscaping.escape(code.name()));
        html.append("</span>").append(HtmlEscaping.escape(name));
        html.append(" (").append(group.size()).append(")</summary>\n");
        html.append("      <div class=\"group-body\">\n");

        if (reference != null) {
            html.append("        <p class=\"reference\">").append(HtmlEscaping.escape(reference));
            html.append("</p>\n");
        }

        if (guidance != null) {
            html.append("        <p class=\"guidance\">").append(HtmlEscaping.escape(guidance));
            html.append("</p>\n");
        }

        html.append("        <table>\n");
        html.append("          <colgroup>");
        html.append("<col class=\"severity\"><col class=\"location\"><col class=\"message\">");
        html.append("</colgroup>\n");
        html.append("          <tr><th>Severity</th><th>Location</th><th>Message</th></tr>\n");

        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFindingRow(html, f, repositoryUrl));

        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </details>\n");
    }

    private static void appendFindingRow(final StringBuilder html, final Finding finding,
                                         final String repositoryUrl) {
        final String severityClass = "sev-" + finding.severity().name().toLowerCase(Locale.ROOT);
        final String location = HtmlFindingLocation.format(finding);
        final String locationHtml = HtmlFindingLocation.toHtml(finding, location, repositoryUrl);

        html.append("          <tr>");
        html.append("<td class=\"").append(severityClass).append("\">");
        html.append(finding.severity().name()).append("</td>");
        html.append("<td class=\"location\">").append(locationHtml).append("</td>");
        html.append("<td>").append(HtmlEscaping.escape(finding.message())).append("</td>");
        html.append("</tr>\n");
    }
}
