package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.fiftieshousewife.cleancode.core.HtmlEscaping.escape;

final class HtmlFindingsRenderer {

    private HtmlFindingsRenderer() {}

    static void appendFindingsByCode(final StringBuilder html, final List<Finding> findings,
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
                .forEach(entry -> appendToolRow(html, entry.getKey(), entry.getValue()));

        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private static void appendCodeGroup(final StringBuilder html, final HeuristicCode code,
                                         final List<Finding> group, final String repositoryUrl) {
        final String name = HeuristicDescriptions.name(code);

        html.append("    <details>\n");
        html.append("      <summary><span class=\"code-label\">").append(escape(code.name()));
        html.append("</span>").append(escape(name));
        html.append(" (").append(group.size()).append(")</summary>\n");
        html.append("      <div class=\"group-body\">\n");

        appendOptionalParagraph(html, "reference", HeuristicDescriptions.reference(code));
        appendOptionalParagraph(html, "guidance", HeuristicDescriptions.guidance(code));

        html.append("        <table>\n");
        html.append("          <tr><th>Severity</th><th>Location</th><th>Message</th></tr>\n");

        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFindingRow(html, f, repositoryUrl));

        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </details>\n");
    }

    private static void appendOptionalParagraph(final StringBuilder html, final String cssClass,
                                                  final String text) {
        if (text == null) {
            return;
        }
        html.append("        <p class=\"").append(cssClass).append("\">");
        html.append(escape(text)).append("</p>\n");
    }

    private static void appendFindingRow(final StringBuilder html, final Finding finding,
                                          final String repositoryUrl) {
        final String severityClass = "sev-" + finding.severity().name().toLowerCase(Locale.ROOT);
        final String location = formatLocation(finding);
        final String locationHtml = buildLocationHtml(finding, location, repositoryUrl);

        html.append("          <tr>");
        html.append("<td class=\"").append(severityClass).append("\">");
        html.append(finding.severity().name()).append("</td>");
        html.append("<td class=\"location\">").append(locationHtml).append("</td>");
        html.append("<td>").append(escape(finding.message())).append("</td>");
        html.append("</tr>\n");
    }

    private static String buildLocationHtml(final Finding finding, final String location,
                                             final String repositoryUrl) {
        if (finding.sourceFile() == null || repositoryUrl == null || repositoryUrl.isBlank()) {
            return escape(location);
        }
        final String baseUrl = repositoryUrl.endsWith("/")
                ? repositoryUrl.substring(0, repositoryUrl.length() - 1) : repositoryUrl;
        final String relativePath = relativiseSourceFile(finding.sourceFile());
        final String fileUrl = baseUrl + "/" + relativePath;
        final String linkedUrl = finding.startLine() > 0
                ? fileUrl + "#L" + finding.startLine() : fileUrl;
        return "<a href=\"" + escape(linkedUrl) + "\">" + escape(location) + "</a>";
    }

    private static String relativiseSourceFile(final String sourceFile) {
        final int srcIdx = sourceFile.indexOf("src/");
        if (srcIdx > 0) {
            return sourceFile.substring(srcIdx);
        }
        return sourceFile;
    }

    private static void appendToolRow(final StringBuilder html, final String tool, final Long count) {
        html.append("        <tr><td>").append(escape(tool));
        html.append("</td><td>").append(count).append("</td></tr>\n");
    }

    private static String formatLocation(final Finding finding) {
        if (finding.sourceFile() == null) {
            return "(project)";
        }
        final String file = shortenPath(finding.sourceFile());
        if (finding.startLine() > 0) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    private static String shortenPath(final String path) {
        final int srcIdx = path.indexOf("src/main/java/");
        if (srcIdx >= 0) {
            return path.substring(srcIdx + "src/main/java/".length());
        }
        return path;
    }
}
