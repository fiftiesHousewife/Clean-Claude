package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HtmlReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneOffset.UTC);

    private HtmlReportWriter() {}

    public static void write(AggregatedReport report, Path outputFile) throws IOException {
        write(report, outputFile, "");
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl) throws IOException {
        final Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, render(report, repositoryUrl));
    }

    private static String render(AggregatedReport report, String repositoryUrl) {
        final StringBuilder html = new StringBuilder();
        appendDocumentStart(html, report);
        appendSeveritySummary(html, report);

        if (report.findings().isEmpty()) {
            html.append("    <p class=\"clean\">No violations found. The code is clean.</p>\n");
        } else {
            appendFindingsByCode(html, report.findings(), repositoryUrl);
            appendToolSummary(html, report.findings());
        }

        appendFooter(html, report);
        appendDocumentEnd(html);
        return html.toString();
    }

    private static void appendDocumentStart(StringBuilder html, AggregatedReport report) {
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Clean Code Report — ").append(escape(report.projectName()));
        html.append("</title>\n");
        appendStyles(html);
        html.append("</head>\n<body>\n");
        html.append("  <header>\n");
        html.append("    <h1>Clean Code Analysis</h1>\n");
        html.append("    <p>").append(escape(report.projectName()));
        if (report.projectVersion() != null) {
            html.append(" v").append(escape(report.projectVersion()));
        }
        html.append("</p>\n");
        html.append("  </header>\n");
        html.append("  <main>\n");
    }

    private static void appendStyles(StringBuilder html) {
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', ");
        html.append("Roboto, sans-serif; color: #333; background: #f5f5f5; }\n");
        html.append("    header { background: #1a1a2e; color: #fff; padding: 2rem; }\n");
        html.append("    header h1 { font-size: 1.5rem; margin-bottom: 0.25rem; }\n");
        html.append("    header p { opacity: 0.8; font-size: 0.95rem; }\n");
        html.append("    main { max-width: 960px; margin: 2rem auto; padding: 0 1rem; }\n");
        html.append("    .summary { display: flex; gap: 1rem; margin-bottom: 2rem; }\n");
        html.append("    .summary .badge { padding: 0.75rem 1.25rem; border-radius: 6px; ");
        html.append("font-weight: 600; font-size: 1.1rem; color: #fff; }\n");
        html.append("    .badge.error { background: #c0392b; }\n");
        html.append("    .badge.warning { background: #e67e22; }\n");
        html.append("    .badge.info { background: #95a5a6; }\n");
        html.append("    details { background: #fff; border: 1px solid #ddd; border-radius: 6px; ");
        html.append("margin-bottom: 1rem; }\n");
        html.append("    summary { cursor: pointer; padding: 1rem; font-weight: 600; ");
        html.append("font-size: 1rem; }\n");
        html.append("    summary:hover { background: #fafafa; }\n");
        html.append("    .code-label { font-family: 'SF Mono', 'Fira Code', monospace; ");
        html.append("font-size: 0.85rem; color: #555; margin-right: 0.5rem; }\n");
        html.append("    .group-body { padding: 0 1rem 1rem 1rem; }\n");
        html.append("    .reference { font-size: 0.85rem; color: #777; ");
        html.append("margin-bottom: 0.5rem; font-style: italic; }\n");
        html.append("    .guidance { font-size: 0.9rem; color: #555; line-height: 1.5; ");
        html.append("margin-bottom: 0.75rem; border-left: 3px solid #ddd; padding-left: 0.75rem; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }\n");
        html.append("    th, td { text-align: left; padding: 0.4rem 0.6rem; ");
        html.append("border-bottom: 1px solid #eee; }\n");
        html.append("    th { color: #777; font-weight: 500; font-size: 0.8rem; ");
        html.append("text-transform: uppercase; }\n");
        html.append("    .sev-error { color: #c0392b; font-weight: 600; }\n");
        html.append("    .sev-warning { color: #e67e22; font-weight: 600; }\n");
        html.append("    .sev-info { color: #95a5a6; }\n");
        html.append("    .location a { color: #2980b9; text-decoration: none; }\n");
        html.append("    .location a:hover { text-decoration: underline; }\n");
        html.append("    .location { font-family: 'SF Mono', 'Fira Code', monospace; ");
        html.append("font-size: 0.85rem; }\n");
        html.append("    .tool-summary { background: #fff; border: 1px solid #ddd; ");
        html.append("border-radius: 6px; padding: 1rem; margin-bottom: 1rem; }\n");
        html.append("    .tool-summary h2 { font-size: 1rem; margin-bottom: 0.5rem; }\n");
        html.append("    .clean { font-size: 1.1rem; color: #27ae60; font-weight: 600; }\n");
        html.append("    footer { text-align: center; padding: 2rem; font-size: 0.8rem; ");
        html.append("color: #999; }\n");
        html.append("  </style>\n");
    }

    private static void appendSeveritySummary(StringBuilder html, AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        final int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        final int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        final int info = bySeverity.getOrDefault(Severity.INFO, List.of()).size();

        html.append("    <div class=\"summary\">\n");
        html.append("      <span class=\"badge error\">").append(errors).append(" errors</span>\n");
        html.append("      <span class=\"badge warning\">").append(warnings);
        html.append(" warnings</span>\n");
        html.append("      <span class=\"badge info\">").append(info).append(" info</span>\n");
        html.append("    </div>\n");
    }

    private static void appendFindingsByCode(StringBuilder html, List<Finding> findings,
                                               String repositoryUrl) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> appendCodeGroup(html, entry.getKey(), entry.getValue(), repositoryUrl));
    }

    private static void appendCodeGroup(StringBuilder html, HeuristicCode code,
                                         List<Finding> group, String repositoryUrl) {
        final String name = HeuristicDescriptions.name(code);
        final String reference = HeuristicDescriptions.reference(code);
        final String guidance = HeuristicDescriptions.guidance(code);

        html.append("    <details>\n");
        html.append("      <summary><span class=\"code-label\">").append(escape(code.name()));
        html.append("</span>").append(escape(name));
        html.append(" (").append(group.size()).append(")</summary>\n");
        html.append("      <div class=\"group-body\">\n");

        if (reference != null) {
            html.append("        <p class=\"reference\">").append(escape(reference));
            html.append("</p>\n");
        }

        if (guidance != null) {
            html.append("        <p class=\"guidance\">").append(escape(guidance));
            html.append("</p>\n");
        }

        html.append("        <table>\n");
        html.append("          <tr><th>Severity</th><th>Location</th><th>Message</th></tr>\n");

        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFindingRow(html, f, repositoryUrl));

        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </details>\n");
    }

    private static void appendFindingRow(StringBuilder html, Finding finding,
                                          String repositoryUrl) {
        final String severityClass = "sev-" + finding.severity().name().toLowerCase();
        final String location = formatLocation(finding);
        final String locationHtml = buildLocationHtml(finding, location, repositoryUrl);

        html.append("          <tr>");
        html.append("<td class=\"").append(severityClass).append("\">");
        html.append(finding.severity().name()).append("</td>");
        html.append("<td class=\"location\">").append(locationHtml).append("</td>");
        html.append("<td>").append(escape(finding.message())).append("</td>");
        html.append("</tr>\n");
    }

    private static String buildLocationHtml(Finding finding, String location,
                                             String repositoryUrl) {
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

    private static String relativiseSourceFile(String sourceFile) {
        final int srcIdx = sourceFile.indexOf("src/");
        if (srcIdx > 0) {
            return sourceFile.substring(srcIdx);
        }
        return sourceFile;
    }

    private static void appendToolSummary(StringBuilder html, List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));

        html.append("    <div class=\"tool-summary\">\n");
        html.append("      <h2>Sources</h2>\n");
        html.append("      <table>\n");
        html.append("        <tr><th>Tool</th><th>Findings</th></tr>\n");

        byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    html.append("        <tr><td>").append(escape(entry.getKey()));
                    html.append("</td><td>").append(entry.getValue()).append("</td></tr>\n");
                });

        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private static void appendFooter(StringBuilder html, AggregatedReport report) {
        final String timestamp = TIMESTAMP_FORMAT.format(report.generatedAt());
        html.append("  </main>\n");
        html.append("  <footer>\n");
        html.append("    <p>Generated by Clean Code Plugin &mdash; ").append(escape(timestamp));
        html.append("</p>\n");
        html.append("    <p>").append(report.findings().size()).append(" total findings</p>\n");
        html.append("  </footer>\n");
    }

    private static void appendDocumentEnd(StringBuilder html) {
        html.append("</body>\n</html>\n");
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

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
