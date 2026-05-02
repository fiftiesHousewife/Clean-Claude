package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

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
        write(report, outputFile, "", null, "vscode");
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl) throws IOException {
        write(report, outputFile, repositoryUrl, null, "vscode");
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl, Path projectRoot, String ideUrlScheme) throws IOException {
        final Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, render(report, repositoryUrl, projectRoot, ideUrlScheme));
    }

    private static String render(AggregatedReport report, String repositoryUrl,
                                  Path projectRoot, String ideUrlScheme) {
        final StringBuilder html = new StringBuilder();
        appendDocumentStart(html, report);
        if (!report.findings().isEmpty()) {
            appendIdePicker(html, ideUrlScheme);
        }
        appendSeveritySummary(html, report);

        if (report.findings().isEmpty()) {
            html.append("    <p class=\"clean\">No violations found. The code is clean.</p>\n");
        } else {
            appendFindingsByCode(html, report.findings(), repositoryUrl, projectRoot, ideUrlScheme);
            appendToolSummary(html, report.findings());
        }

        appendFooter(html, report);
        if (!report.findings().isEmpty()) {
            appendInteractionScript(html, projectRoot);
        }
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
        html.append("    main { max-width: min(1800px, 96vw); margin: 2rem auto; padding: 0 1rem; }\n");
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
        html.append("    .group-body { overflow-x: auto; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; font-size: 0.9rem; ");
        html.append("table-layout: fixed; }\n");
        html.append("    th, td { text-align: left; padding: 0.4rem 0.6rem; ");
        html.append("border-bottom: 1px solid #eee; vertical-align: top; }\n");
        html.append("    col.severity { width: 5rem; }\n");
        html.append("    col.location { width: 38%; }\n");
        html.append("    col.message { width: auto; }\n");
        html.append("    td:first-child { white-space: nowrap; }\n");
        html.append("    td:nth-child(2) { font-size: 0.85rem; overflow-wrap: anywhere; word-break: break-all; }\n");
        html.append("    td:nth-child(3) { overflow-wrap: anywhere; word-break: break-word; }\n");
        html.append("    th { color: #777; font-weight: 500; font-size: 0.8rem; ");
        html.append("text-transform: uppercase; }\n");
        html.append("    .sev-error { color: #c0392b; font-weight: 600; }\n");
        html.append("    .sev-warning { color: #e67e22; font-weight: 600; }\n");
        html.append("    .sev-info { color: #95a5a6; }\n");
        html.append("    .location a { color: #2980b9; text-decoration: none; }\n");
        html.append("    .location a:hover { text-decoration: underline; }\n");
        html.append("    .location .repo-link { font-size: 0.75rem; color: #999; ");
        html.append("margin-left: 0.4rem; }\n");
        html.append("    .location .copy-link { cursor: pointer; user-select: none; ");
        html.append("font-size: 0.75rem; color: #999; margin-left: 0.4rem; }\n");
        html.append("    .location .copy-link:hover { color: #2980b9; }\n");
        html.append("    .location .copy-link.copied { color: #27ae60; }\n");
        html.append("    .location { font-family: 'SF Mono', 'Fira Code', monospace; ");
        html.append("font-size: 0.85rem; }\n");
        html.append("    .ide-picker { font-size: 0.85rem; color: #555; ");
        html.append("margin-bottom: 1rem; padding: 0.5rem 0.75rem; background: #fff; ");
        html.append("border: 1px solid #ddd; border-radius: 6px; }\n");
        html.append("    .ide-picker select { font-size: 0.85rem; padding: 0.2rem 0.4rem; }\n");
        html.append("    .tool-summary { background: #fff; border: 1px solid #ddd; ");
        html.append("border-radius: 6px; padding: 1rem; margin-bottom: 1rem; }\n");
        html.append("    .tool-summary h2 { font-size: 1rem; margin-bottom: 0.5rem; }\n");
        html.append("    .clean { font-size: 1.1rem; color: #27ae60; font-weight: 600; }\n");
        html.append("    footer { text-align: center; padding: 2rem; font-size: 0.8rem; ");
        html.append("color: #999; }\n");
        html.append("  </style>\n");
    }

    private static void appendIdePicker(StringBuilder html, String ideUrlScheme) {
        html.append("    <div class=\"ide-picker\">\n");
        html.append("      Open clicked links in: <select id=\"ide-scheme\">");
        for (final String s : new String[] {"vscode", "idea", "cursor"}) {
            final String selected = s.equals(ideUrlScheme) ? " selected" : "";
            html.append("<option value=\"").append(s).append("\"").append(selected).append(">");
            html.append(s).append("</option>");
        }
        html.append("</select>\n");
        html.append("      <span style=\"color:#999;margin-left:1rem;\">");
        html.append("If clicking does nothing, your IDE's URL handler isn't registered — ");
        html.append("use the &#128203; icon to copy a CLI command instead.</span>\n");
        html.append("    </div>\n");
    }

    private static void appendInteractionScript(StringBuilder html, Path projectRoot) {
        final String root = projectRoot == null ? "" : projectRoot.toAbsolutePath().toString();
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      const ROOT = ").append(jsString(root)).append(";\n");
        html.append("      const select = document.getElementById('ide-scheme');\n");
        html.append("      const stored = localStorage.getItem('cleanCodeIdeScheme');\n");
        html.append("      if (stored) select.value = stored;\n");
        html.append("      function buildIdeUrl(scheme, path, line) {\n");
        html.append("        switch (scheme) {\n");
        html.append("          case 'vscode': return 'vscode://file' + path + ':' + line + ':1';\n");
        html.append("          case 'cursor': return 'cursor://file' + path + ':' + line + ':1';\n");
        html.append("          case 'idea':   return 'idea://open?file=' + encodeURIComponent(path) + '&line=' + line;\n");
        html.append("        }\n");
        html.append("        return 'file://' + path;\n");
        html.append("      }\n");
        html.append("      function buildCliCommand(scheme, path, line) {\n");
        html.append("        switch (scheme) {\n");
        html.append("          case 'vscode': return 'code -g ' + path + ':' + line;\n");
        html.append("          case 'cursor': return 'cursor -g ' + path + ':' + line;\n");
        html.append("          case 'idea':   return 'idea --line ' + line + ' ' + path;\n");
        html.append("        }\n");
        html.append("        return path + ':' + line;\n");
        html.append("      }\n");
        html.append("      function rewire() {\n");
        html.append("        const scheme = select.value;\n");
        html.append("        document.querySelectorAll('a.ide-link').forEach(a => {\n");
        html.append("          const path = a.dataset.path; const line = a.dataset.line;\n");
        html.append("          a.href = buildIdeUrl(scheme, path, line);\n");
        html.append("        });\n");
        html.append("        document.querySelectorAll('span.copy-link').forEach(span => {\n");
        html.append("          const path = span.dataset.path; const line = span.dataset.line;\n");
        html.append("          span.title = 'Copy: ' + buildCliCommand(scheme, path, line);\n");
        html.append("        });\n");
        html.append("      }\n");
        html.append("      select.addEventListener('change', () => {\n");
        html.append("        localStorage.setItem('cleanCodeIdeScheme', select.value);\n");
        html.append("        rewire();\n");
        html.append("      });\n");
        html.append("      document.addEventListener('click', e => {\n");
        html.append("        const span = e.target.closest('span.copy-link');\n");
        html.append("        if (!span) return;\n");
        html.append("        const cmd = buildCliCommand(select.value, span.dataset.path, span.dataset.line);\n");
        html.append("        navigator.clipboard.writeText(cmd).then(() => {\n");
        html.append("          span.classList.add('copied');\n");
        html.append("          const original = span.textContent;\n");
        html.append("          span.textContent = '✓';\n");
        html.append("          setTimeout(() => { span.classList.remove('copied'); span.textContent = original; }, 1200);\n");
        html.append("        });\n");
        html.append("      });\n");
        html.append("      rewire();\n");
        html.append("    })();\n");
        html.append("  </script>\n");
    }

    private static String jsString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
                                               String repositoryUrl, Path projectRoot,
                                               String ideUrlScheme) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> appendCodeGroup(html, entry.getKey(), entry.getValue(),
                        repositoryUrl, projectRoot, ideUrlScheme));
    }

    private static void appendCodeGroup(StringBuilder html, HeuristicCode code,
                                         List<Finding> group, String repositoryUrl,
                                         Path projectRoot, String ideUrlScheme) {
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
        html.append("          <colgroup>");
        html.append("<col class=\"severity\"><col class=\"location\"><col class=\"message\">");
        html.append("</colgroup>\n");
        html.append("          <tr><th>Severity</th><th>Location</th><th>Message</th></tr>\n");

        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFindingRow(html, f, repositoryUrl, projectRoot, ideUrlScheme));

        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </details>\n");
    }

    private static void appendFindingRow(StringBuilder html, Finding finding,
                                          String repositoryUrl, Path projectRoot,
                                          String ideUrlScheme) {
        final String severityClass = "sev-" + finding.severity().name().toLowerCase();
        final String location = formatLocation(finding);
        final String locationHtml = buildLocationHtml(finding, location, repositoryUrl,
                projectRoot, ideUrlScheme);

        html.append("          <tr>");
        html.append("<td class=\"").append(severityClass).append("\">");
        html.append(finding.severity().name()).append("</td>");
        html.append("<td class=\"location\">").append(locationHtml).append("</td>");
        html.append("<td>").append(escape(finding.message())).append("</td>");
        html.append("</tr>\n");
    }

    private static String buildLocationHtml(Finding finding, String location,
                                             String repositoryUrl, Path projectRoot,
                                             String ideUrlScheme) {
        if (finding.sourceFile() == null) {
            return escape(location);
        }
        final String absolutePath = projectRoot == null
                ? finding.sourceFile() : absolutise(finding.sourceFile(), projectRoot);
        final int line = finding.startLine() > 0 ? finding.startLine() : 1;
        final String ideUrl = buildIdeUrl(absolutePath, line, ideUrlScheme);
        final String dataAttrs = " data-path=\"" + escape(absolutePath) + "\""
                + " data-line=\"" + line + "\"";
        final String primary = ideUrl != null
                ? "<a class=\"ide-link\" href=\"" + escape(ideUrl) + "\"" + dataAttrs
                        + ">" + escape(location) + "</a>"
                : "<span class=\"ide-link\"" + dataAttrs + ">" + escape(location) + "</span>";
        final String copyIcon = "<span class=\"copy-link\"" + dataAttrs
                + " title=\"Copy CLI command to clipboard\">&#128203;</span>";
        final String repoLink = buildRepositoryLink(finding, repositoryUrl);
        return primary + copyIcon + (repoLink == null ? "" : " " + repoLink);
    }

    private static String buildIdeUrl(String absolutePath, int line, String ideUrlScheme) {
        if (ideUrlScheme == null || ideUrlScheme.isBlank()
                || "none".equalsIgnoreCase(ideUrlScheme)) {
            return null;
        }
        return switch (ideUrlScheme.toLowerCase()) {
            case "vscode" -> "vscode://file" + absolutePath + ":" + line + ":1";
            case "idea", "intellij" -> "idea://open?file=" + absolutePath + "&line=" + line;
            case "cursor" -> "cursor://file" + absolutePath + ":" + line + ":1";
            default -> "file://" + absolutePath;
        };
    }

    private static String buildRepositoryLink(Finding finding, String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return null;
        }
        final String baseUrl = repositoryUrl.endsWith("/")
                ? repositoryUrl.substring(0, repositoryUrl.length() - 1) : repositoryUrl;
        final String relativePath = relativiseSourceFile(finding.sourceFile());
        final String fileUrl = baseUrl + "/" + relativePath;
        final String linkedUrl = finding.startLine() > 0
                ? fileUrl + "#L" + finding.startLine() : fileUrl;
        return "<a class=\"repo-link\" href=\"" + escape(linkedUrl)
                + "\" title=\"View on GitHub\">[gh]</a>";
    }

    private static String absolutise(String sourceFile, Path projectRoot) {
        if (sourceFile.startsWith("/")) {
            return sourceFile;
        }
        return projectRoot.resolve(sourceFile).toAbsolutePath().toString();
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
