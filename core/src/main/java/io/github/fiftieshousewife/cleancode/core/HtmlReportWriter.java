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
        write(report, outputFile, "", null, "vscode", List.of());
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl) throws IOException {
        write(report, outputFile, repositoryUrl, null, "vscode", List.of());
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl, Path projectRoot, String ideUrlScheme) throws IOException {
        write(report, outputFile, repositoryUrl, projectRoot, ideUrlScheme, List.of());
    }

    public static void write(AggregatedReport report, Path outputFile,
                              String repositoryUrl, Path projectRoot, String ideUrlScheme,
                              List<SourceState> sourceStates) throws IOException {
        final Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, render(report, repositoryUrl, projectRoot, ideUrlScheme, sourceStates));
    }

    private static String render(AggregatedReport report, String repositoryUrl,
                                  Path projectRoot, String ideUrlScheme,
                                  List<SourceState> sourceStates) {
        final StringBuilder html = new StringBuilder();
        appendDocumentStart(html, report);
        appendStagingBar(html);
        if (!report.findings().isEmpty()) {
            appendIdePicker(html, ideUrlScheme);
        }
        appendSeveritySummary(html, report);

        if (report.findings().isEmpty()) {
            html.append("    <p class=\"clean\">No violations found. The code is clean.</p>\n");
        } else {
            appendFindingsByCode(html, report.findings(), repositoryUrl, projectRoot, ideUrlScheme);
        }
        appendToolSummary(html, report.findings(), sourceStates);
        appendOptionalRulesSummary(html);

        appendFooter(html, report);
        appendStagingModal(html);
        appendFeedbackModal(html);
        if (!report.findings().isEmpty()) {
            appendInteractionScript(html, projectRoot);
            appendSeverityFilterScript(html);
            appendSnippetToggleScript(html);
        }
        appendFeedbackScript(html);
        appendStagingScript(html);
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
        html.append("    <div class=\"header-row\">\n");
        html.append("      <div>\n");
        html.append("        <h1>Clean Code Analysis</h1>\n");
        html.append("        <p>").append(escape(report.projectName()));
        if (report.projectVersion() != null) {
            html.append(" v").append(escape(report.projectVersion()));
        }
        html.append("</p>\n");
        html.append("      </div>\n");
        html.append("      <button type=\"button\" class=\"feedback-btn\" id=\"open-feedback\" ");
        html.append("title=\"Capture a thought about this finding or the report itself — saved to ");
        html.append("clean-code-feedback.md\">&#128172; Request a change</button>\n");
        html.append("    </div>\n");
        html.append("  </header>\n");
        html.append("  <main>\n");
    }

    private static void appendSnippetStyles(final StringBuilder html) {
        html.append("    tr.snippet-row td { padding: 0; background: #fafafa; ");
        html.append("border-bottom: 1px solid #eee; }\n");
        html.append("    tr.snippet-row pre { margin: 0; padding: 0.6rem 0.9rem; ");
        html.append("font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.78rem; ");
        html.append("line-height: 1.45; overflow-x: auto; color: #333; ");
        html.append("background: transparent; }\n");
        html.append("    tr.snippet-row .ln-row { display: block; ");
        html.append("white-space: pre; }\n");
        html.append("    tr.snippet-row .ln { display: inline-block; width: 2.5rem; ");
        html.append("text-align: right; padding-right: 0.7rem; color: #bbb; user-select: none; }\n");
        html.append("    tr.snippet-row .ln-row.focal { background: #fff8dc; ");
        html.append("margin: 0 -0.9rem; padding: 0 0.9rem; }\n");
        html.append("    tr.snippet-row .ln-row.focal .ln { color: #888; font-weight: 600; }\n");
        html.append("    .snippet-toggle { display: inline-block; margin-left: 0.4rem; ");
        html.append("font-size: 0.75rem; color: #2980b9; cursor: pointer; user-select: none; }\n");
        html.append("    .snippet-toggle:hover { text-decoration: underline; }\n");
    }

    private static void appendStyles(StringBuilder html) {
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', ");
        html.append("Roboto, sans-serif; color: #333; background: #f5f5f5; }\n");
        html.append("    header { background: #1a1a2e; color: #fff; padding: 2rem; }\n");
        html.append("    header h1 { font-size: 1.5rem; margin-bottom: 0.25rem; }\n");
        html.append("    header p { opacity: 0.8; font-size: 0.95rem; }\n");
        html.append("    .header-row { display: flex; justify-content: space-between; ");
        html.append("align-items: flex-start; gap: 1rem; flex-wrap: wrap; }\n");
        html.append("    .feedback-btn { background: #34495e; color: #fff; ");
        html.append("border: 1px solid #4a627a; border-radius: 6px; padding: 0.55rem 0.9rem; ");
        html.append("font-size: 0.85rem; cursor: pointer; font-family: inherit; }\n");
        html.append("    .feedback-btn:hover { background: #46627e; }\n");
        html.append("    main { max-width: min(1800px, 96vw); margin: 2rem auto; padding: 0 1rem; }\n");
        html.append("    .summary { display: flex; gap: 1rem; margin-bottom: 2rem; }\n");
        html.append("    .summary .badge { padding: 0.75rem 1.25rem; border-radius: 6px; ");
        html.append("font-weight: 600; font-size: 1.1rem; color: #fff; cursor: pointer; ");
        html.append("border: 0; font-family: inherit; user-select: none; ");
        html.append("transition: opacity 0.15s, transform 0.15s; }\n");
        html.append("    .summary .badge:hover { transform: scale(1.03); }\n");
        html.append("    .summary .badge.off { opacity: 0.35; text-decoration: line-through; }\n");
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
        html.append("    col.confidence { width: 5.5rem; }\n");
        html.append("    col.location { width: 35%; }\n");
        html.append("    col.message { width: auto; }\n");
        html.append("    .confidence-pill { display: inline-block; font-size: 0.7rem; ");
        html.append("font-weight: 600; padding: 0.1rem 0.45rem; border-radius: 999px; ");
        html.append("text-transform: uppercase; letter-spacing: 0.04em; }\n");
        html.append("    .confidence-pill.high { background: #27ae60; color: #fff; }\n");
        html.append("    .confidence-pill.medium { background: #fff; color: #d35400; ");
        html.append("border: 1px solid #d35400; }\n");
        html.append("    .confidence-pill.low { background: #fff; color: #95a5a6; ");
        html.append("border: 1px solid #bdc3c7; }\n");
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
        appendSnippetStyles(html);
        appendStagingStyles(html);
        html.append("  </style>\n");
    }

    private static void appendStagingStyles(final StringBuilder html) {
        html.append("    col.actions { width: 11rem; }\n");
        html.append("    td.actions { white-space: nowrap; text-align: right; }\n");
        html.append("    .code-actions { margin-left: 0.75rem; display: inline-flex; gap: 0.4rem; }\n");
        html.append("    .action-btn { font-size: 0.75rem; padding: 0.2rem 0.5rem; ");
        html.append("border: 1px solid #ccc; background: #fff; color: #333; ");
        html.append("border-radius: 4px; cursor: pointer; font-family: inherit; }\n");
        html.append("    .action-btn:hover { background: #f0f0f0; border-color: #999; }\n");
        html.append("    .action-btn:disabled { opacity: 0.4; cursor: not-allowed; }\n");
        html.append("    .action-btn.staged { background: #fff8dc; border-color: #d4a017; }\n");
        html.append("    #staging-bar { position: sticky; top: 0; z-index: 10; ");
        html.append("background: #fff8dc; border-bottom: 2px solid #d4a017; ");
        html.append("padding: 0.75rem 1rem; display: none; align-items: center; gap: 1rem; ");
        html.append("box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    #staging-bar.has-changes { display: flex; }\n");
        html.append("    #staging-bar.server-down { background: #fce4e4; border-color: #c0392b; }\n");
        html.append("    #staging-bar .pending-count { font-weight: 600; }\n");
        html.append("    #staging-bar button { font-size: 0.85rem; padding: 0.4rem 0.8rem; ");
        html.append("border: 1px solid #ccc; border-radius: 4px; cursor: pointer; ");
        html.append("font-family: inherit; }\n");
        html.append("    #staging-bar .confirm-btn { background: #27ae60; color: #fff; ");
        html.append("border-color: #27ae60; }\n");
        html.append("    #staging-bar .confirm-btn:hover { background: #219653; }\n");
        html.append("    #staging-bar .discard-btn { background: #fff; color: #c0392b; ");
        html.append("border-color: #c0392b; }\n");
        html.append("    #staging-bar .discard-btn:hover { background: #fce4e4; }\n");
        html.append("    #staging-bar .stop-btn { background: #fff; color: #34495e; ");
        html.append("border-color: #7f8c8d; }\n");
        html.append("    #staging-bar .stop-btn:hover { background: #ecf0f1; }\n");
        html.append("    #staging-bar.server-stopped { background: #ecf0f1; ");
        html.append("border-color: #7f8c8d; display: flex; }\n");
        html.append("    #staging-bar.server-stopped button { display: none; }\n");
        html.append("    #staging-bar.server-stopped details { color: #34495e; ");
        html.append("font-weight: 600; }\n");
        html.append("    #staging-bar details { background: transparent; border: 0; ");
        html.append("margin: 0; flex: 1; }\n");
        html.append("    #staging-bar summary { padding: 0; font-size: 0.85rem; ");
        html.append("font-weight: normal; color: #555; }\n");
        html.append("    #staging-bar .pending-list { font-size: 0.8rem; ");
        html.append("color: #555; margin-top: 0.5rem; max-height: 30vh; overflow-y: auto; }\n");
        html.append("    #staging-bar .pending-list .row { padding: 0.2rem 0; ");
        html.append("border-bottom: 1px dashed #d4a017; }\n");
        html.append("    .modal-backdrop { position: fixed; inset: 0; ");
        html.append("background: rgba(0,0,0,0.4); display: none; ");
        html.append("align-items: center; justify-content: center; z-index: 100; }\n");
        html.append("    .modal-backdrop.open { display: flex; }\n");
        html.append("    .modal { background: #fff; border-radius: 6px; padding: 1.5rem; ");
        html.append("width: min(500px, 90vw); box-shadow: 0 8px 24px rgba(0,0,0,0.2); }\n");
        html.append("    .modal h3 { font-size: 1.05rem; margin-bottom: 0.5rem; }\n");
        html.append("    .modal .summary-line { font-size: 0.85rem; color: #555; ");
        html.append("margin-bottom: 1rem; font-family: 'SF Mono', monospace; }\n");
        html.append("    .modal label { display: block; font-size: 0.85rem; ");
        html.append("color: #555; margin-bottom: 0.25rem; }\n");
        html.append("    .modal textarea, .modal input { width: 100%; padding: 0.5rem; ");
        html.append("font: inherit; font-size: 0.9rem; border: 1px solid #ccc; ");
        html.append("border-radius: 4px; margin-bottom: 1rem; }\n");
        html.append("    .modal textarea { resize: vertical; min-height: 4rem; }\n");
        html.append("    .modal .modal-actions { display: flex; gap: 0.5rem; ");
        html.append("justify-content: flex-end; }\n");
        html.append("    .modal button { font-size: 0.9rem; padding: 0.4rem 1rem; ");
        html.append("border: 1px solid #ccc; border-radius: 4px; cursor: pointer; ");
        html.append("font-family: inherit; background: #fff; }\n");
        html.append("    .modal button.primary { background: #2980b9; color: #fff; ");
        html.append("border-color: #2980b9; }\n");
        html.append("    .modal button.primary:disabled { opacity: 0.4; cursor: not-allowed; }\n");
        html.append("    .modal .threshold-row { display: flex; gap: 1rem; align-items: end; }\n");
        html.append("    .modal .threshold-row > div { flex: 1; }\n");
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

    private static void appendSnippetToggleScript(final StringBuilder html) {
        html.append("  <script>\n");
        html.append("    document.addEventListener('click', e => {\n");
        html.append("      const toggle = e.target.closest('[data-action=\"toggle-snippet\"]');\n");
        html.append("      if (!toggle) return;\n");
        html.append("      const row = toggle.closest('tr');\n");
        html.append("      const next = row.nextElementSibling;\n");
        html.append("      if (!next || !next.classList.contains('snippet-row')) return;\n");
        html.append("      const open = !next.hidden;\n");
        html.append("      next.hidden = open;\n");
        html.append("      toggle.innerHTML = open ? '&#9656; snippet' : '&#9662; hide';\n");
        html.append("    });\n");
        html.append("  </script>\n");
    }

    private static void appendSeverityFilterScript(final StringBuilder html) {
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      const STORAGE_KEY = 'cleanCodeSeverityFilter';\n");
        html.append("      const bar = document.getElementById('severity-filter');\n");
        html.append("      if (!bar) return;\n");
        html.append("      const badges = bar.querySelectorAll('[data-severity-toggle]');\n");
        html.append("      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');\n");
        html.append("      const visible = stored || { error: true, warning: true, info: true };\n");
        html.append("      function apply() {\n");
        html.append("        localStorage.setItem(STORAGE_KEY, JSON.stringify(visible));\n");
        html.append("        badges.forEach(b => b.classList.toggle('off', !visible[b.dataset.severityToggle]));\n");
        html.append("        document.querySelectorAll('tr[data-severity]').forEach(row => {\n");
        html.append("          const show = visible[row.dataset.severity];\n");
        html.append("          if (row.classList.contains('snippet-row')) {\n");
        html.append("            if (!show) row.hidden = true;\n");
        html.append("            row.style.display = show ? '' : 'none';\n");
        html.append("          } else {\n");
        html.append("            row.style.display = show ? '' : 'none';\n");
        html.append("          }\n");
        html.append("        });\n");
        html.append("        document.querySelectorAll('details[data-code]').forEach(group => {\n");
        html.append("          const rows = Array.from(group.querySelectorAll('tr[data-severity]'))\n");
        html.append("              .filter(r => !r.classList.contains('snippet-row'));\n");
        html.append("          const anyVisible = rows.some(r => r.style.display !== 'none');\n");
        html.append("          group.style.display = anyVisible ? '' : 'none';\n");
        html.append("        });\n");
        html.append("      }\n");
        html.append("      badges.forEach(b => b.addEventListener('click', () => {\n");
        html.append("        const key = b.dataset.severityToggle;\n");
        html.append("        visible[key] = !visible[key];\n");
        html.append("        apply();\n");
        html.append("      }));\n");
        html.append("      apply();\n");
        html.append("    })();\n");
        html.append("  </script>\n");
    }

    private static void appendStagingBar(final StringBuilder html) {
        html.append("  <div id=\"staging-bar\">\n");
        html.append("    <details>\n");
        html.append("      <summary><span class=\"pending-count\">0</span> pending change(s) ");
        html.append("&mdash; <span class=\"server-status\"></span></summary>\n");
        html.append("      <div class=\"pending-list\"></div>\n");
        html.append("    </details>\n");
        html.append("    <button class=\"confirm-btn\" type=\"button\">Confirm &amp; apply</button>\n");
        html.append("    <button class=\"discard-btn\" type=\"button\">Discard</button>\n");
        html.append("    <button class=\"stop-btn\" type=\"button\" title=\"Stop the cleanCodeServe task\">");
        html.append("&#128721; Stop server</button>\n");
        html.append("  </div>\n");
    }

    private static void appendStagingModal(final StringBuilder html) {
        html.append("  <div class=\"modal-backdrop\" id=\"stage-modal\">\n");
        html.append("    <form class=\"modal\" id=\"stage-modal-form\">\n");
        html.append("      <h3 id=\"modal-title\">Stage change</h3>\n");
        html.append("      <p class=\"summary-line\" id=\"modal-summary\"></p>\n");
        html.append("      <div id=\"modal-threshold-row\" class=\"threshold-row\" style=\"display:none\">\n");
        html.append("        <div><label>Current</label><input type=\"text\" id=\"modal-current\" disabled></div>\n");
        html.append("        <div><label>New value</label><input type=\"number\" id=\"modal-new-value\" min=\"1\"></div>\n");
        html.append("      </div>\n");
        html.append("      <label for=\"modal-reason\">Reason (required, min 5 chars)</label>\n");
        html.append("      <textarea id=\"modal-reason\" required minlength=\"5\" ");
        html.append("placeholder=\"Why are you making this change? This goes into the suppression annotation / build script comment.\"></textarea>\n");
        html.append("      <div class=\"modal-actions\">\n");
        html.append("        <button type=\"button\" class=\"cancel-btn\">Cancel</button>\n");
        html.append("        <button type=\"submit\" class=\"primary\" disabled>Stage</button>\n");
        html.append("      </div>\n");
        html.append("    </form>\n");
        html.append("  </div>\n");
    }

    private static void appendFeedbackModal(final StringBuilder html) {
        html.append("  <div class=\"modal-backdrop\" id=\"feedback-modal\">\n");
        html.append("    <form class=\"modal\" id=\"feedback-form\">\n");
        html.append("      <h3>Request a change</h3>\n");
        html.append("      <p class=\"summary-line\">Saved locally to ");
        html.append("<code>clean-code-feedback.md</code> at the project root. Review before commit.</p>\n");
        html.append("      <label for=\"feedback-message\">What would make this report more useful?</label>\n");
        html.append("      <textarea id=\"feedback-message\" required minlength=\"10\" rows=\"5\" ");
        html.append("placeholder=\"Tell us what's missing, wrong, or noisy.\"></textarea>\n");
        html.append("      <div class=\"modal-actions\">\n");
        html.append("        <button type=\"button\" class=\"cancel-btn\">Cancel</button>\n");
        html.append("        <button type=\"submit\" class=\"primary\" disabled>Save feedback</button>\n");
        html.append("      </div>\n");
        html.append("    </form>\n");
        html.append("  </div>\n");
    }

    private static void appendFeedbackScript(final StringBuilder html) {
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      const modal = document.getElementById('feedback-modal');\n");
        html.append("      const form = document.getElementById('feedback-form');\n");
        html.append("      const openBtn = document.getElementById('open-feedback');\n");
        html.append("      if (!openBtn || !modal || !form) return;\n");
        html.append("      const textarea = document.getElementById('feedback-message');\n");
        html.append("      const submit = form.querySelector('button[type=submit]');\n");
        html.append("      const cancelBtn = form.querySelector('.cancel-btn');\n");
        html.append("      function close() { modal.classList.remove('open'); textarea.value = ''; submit.disabled = true; }\n");
        html.append("      openBtn.addEventListener('click', () => {\n");
        html.append("        textarea.value = '';\n");
        html.append("        submit.disabled = true;\n");
        html.append("        modal.classList.add('open');\n");
        html.append("        textarea.focus();\n");
        html.append("      });\n");
        html.append("      textarea.addEventListener('input', () => {\n");
        html.append("        submit.disabled = textarea.value.trim().length < 10;\n");
        html.append("      });\n");
        html.append("      cancelBtn.addEventListener('click', close);\n");
        html.append("      form.addEventListener('submit', e => {\n");
        html.append("        e.preventDefault();\n");
        html.append("        if (textarea.value.trim().length < 10) return;\n");
        html.append("        submit.disabled = true;\n");
        html.append("        submit.textContent = 'Saving...';\n");
        html.append("        fetch('/api/feedback', {\n");
        html.append("          method: 'POST',\n");
        html.append("          headers: { 'Content-Type': 'application/json' },\n");
        html.append("          body: JSON.stringify({ message: textarea.value.trim() })\n");
        html.append("        }).then(r => r.json().then(j => ({ status: r.status, body: j })))\n");
        html.append("          .then(({ status, body }) => {\n");
        html.append("            if (status === 200 && body.success) {\n");
        html.append("              alert('Saved to ' + body.savedTo);\n");
        html.append("              close();\n");
        html.append("              submit.textContent = 'Save feedback';\n");
        html.append("            } else {\n");
        html.append("              alert('Could not save: ' + (body.error || 'unknown'));\n");
        html.append("              submit.disabled = false;\n");
        html.append("              submit.textContent = 'Save feedback';\n");
        html.append("            }\n");
        html.append("          }).catch(err => {\n");
        html.append("            alert('Network error: ' + err.message);\n");
        html.append("            submit.disabled = false;\n");
        html.append("            submit.textContent = 'Save feedback';\n");
        html.append("          });\n");
        html.append("      });\n");
        html.append("    })();\n");
        html.append("  </script>\n");
    }

    private static void appendStagingScript(final StringBuilder html) {
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      const STORAGE_KEY = 'cleanCodePendingChanges';\n");
        html.append("      const bar = document.getElementById('staging-bar');\n");
        html.append("      const countEl = bar.querySelector('.pending-count');\n");
        html.append("      const statusEl = bar.querySelector('.server-status');\n");
        html.append("      const listEl = bar.querySelector('.pending-list');\n");
        html.append("      const confirmBtn = bar.querySelector('.confirm-btn');\n");
        html.append("      const discardBtn = bar.querySelector('.discard-btn');\n");
        html.append("      const stopBtn = bar.querySelector('.stop-btn');\n");
        html.append("      const modal = document.getElementById('stage-modal');\n");
        html.append("      const modalForm = document.getElementById('stage-modal-form');\n");
        html.append("      const modalTitle = document.getElementById('modal-title');\n");
        html.append("      const modalSummary = document.getElementById('modal-summary');\n");
        html.append("      const modalThreshold = document.getElementById('modal-threshold-row');\n");
        html.append("      const modalCurrent = document.getElementById('modal-current');\n");
        html.append("      const modalNewValue = document.getElementById('modal-new-value');\n");
        html.append("      const modalReason = document.getElementById('modal-reason');\n");
        html.append("      const modalSubmit = modalForm.querySelector('button[type=submit]');\n");
        html.append("      let pending = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');\n");
        html.append("      let serverState = null;\n");
        html.append("      let pendingDraft = null;\n");
        html.append("\n");
        html.append("      function load() {\n");
        html.append("        fetch('/api/state').then(r => r.ok ? r.json() : null).then(state => {\n");
        html.append("          serverState = state;\n");
        html.append("          if (state) { statusEl.textContent = 'server connected'; }\n");
        html.append("          else { setServerDown(); }\n");
        html.append("          render();\n");
        html.append("        }).catch(() => { setServerDown(); render(); });\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function setServerDown() {\n");
        html.append("        bar.classList.add('server-down');\n");
        html.append("        statusEl.textContent = 'server not running — start ./gradlew cleanCodeServe to enable';\n");
        html.append("        document.querySelectorAll('.action-btn').forEach(b => {\n");
        html.append("          b.disabled = true;\n");
        html.append("          b.title = 'Run ./gradlew cleanCodeServe to enable';\n");
        html.append("        });\n");
        html.append("        confirmBtn.disabled = true;\n");
        html.append("        stopBtn.disabled = true;\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function setServerStopped() {\n");
        html.append("        bar.classList.remove('server-down');\n");
        html.append("        bar.classList.remove('has-changes');\n");
        html.append("        bar.classList.add('server-stopped');\n");
        html.append("        statusEl.textContent = 'Server stopped — re-run ./gradlew cleanCodeServe to resume';\n");
        html.append("        document.querySelectorAll('.action-btn').forEach(b => {\n");
        html.append("          b.disabled = true;\n");
        html.append("          b.title = 'Server stopped';\n");
        html.append("        });\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function render() {\n");
        html.append("        countEl.textContent = pending.length;\n");
        html.append("        if (pending.length > 0) bar.classList.add('has-changes');\n");
        html.append("        else bar.classList.remove('has-changes');\n");
        html.append("        listEl.innerHTML = pending.map(c => {\n");
        html.append("          const params = Object.entries(c.params).map(([k,v]) => k+'='+v).join(' ');\n");
        html.append("          return '<div class=\"row\">' + esc(c.kind) + ' &middot; ' + esc(params)\n");
        html.append("              + ' &middot; <em>' + esc(c.reason) + '</em></div>';\n");
        html.append("        }).join('');\n");
        html.append("        markStagedButtons();\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function markStagedButtons() {\n");
        html.append("        document.querySelectorAll('.action-btn.staged').forEach(b => b.classList.remove('staged'));\n");
        html.append("        pending.forEach(c => {\n");
        html.append("          let selector;\n");
        html.append("          if (c.kind === 'suppressFinding') {\n");
        html.append("            selector = '.suppress-btn[data-code=\"'+c.params.code+'\"][data-file=\"'+c.params.file+'\"][data-line=\"'+c.params.line+'\"]';\n");
        html.append("          } else if (c.kind === 'disableRecipe') {\n");
        html.append("            selector = '.disable-btn[data-code=\"'+c.params.code+'\"]';\n");
        html.append("          } else if (c.kind === 'tuneThreshold') {\n");
        html.append("            selector = '.tune-btn[data-code=\"'+c.params.code+'\"]';\n");
        html.append("          }\n");
        html.append("          if (selector) document.querySelectorAll(selector).forEach(b => b.classList.add('staged'));\n");
        html.append("          if (c.kind === 'applyRefactoring') {\n");
        html.append("            const file = c.params.file || '';\n");
        html.append("            if (file) {\n");
        html.append("              const sel = '.fix-btn[data-code=\"'+c.params.code+'\"][data-file=\"'+file+'\"]';\n");
        html.append("              document.querySelectorAll(sel).forEach(b => b.classList.add('staged'));\n");
        html.append("            } else {\n");
        html.append("              const sel = '.fix-all-btn[data-code=\"'+c.params.code+'\"]';\n");
        html.append("              document.querySelectorAll(sel).forEach(b => b.classList.add('staged'));\n");
        html.append("            }\n");
        html.append("          }\n");
        html.append("        });\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }\n");
        html.append("\n");
        html.append("      function openModal(draft) {\n");
        html.append("        pendingDraft = draft;\n");
        html.append("        modalTitle.textContent = draft.title;\n");
        html.append("        modalSummary.innerHTML = draft.summary;\n");
        html.append("        modalReason.value = '';\n");
        html.append("        if (draft.kind === 'tuneThreshold') {\n");
        html.append("          modalThreshold.style.display = 'flex';\n");
        html.append("          modalCurrent.value = draft.currentValue;\n");
        html.append("          modalNewValue.value = draft.suggestedValue;\n");
        html.append("        } else {\n");
        html.append("          modalThreshold.style.display = 'none';\n");
        html.append("        }\n");
        html.append("        modal.classList.add('open');\n");
        html.append("        modalReason.focus();\n");
        html.append("        validateModal();\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      function closeModal() { modal.classList.remove('open'); pendingDraft = null; }\n");
        html.append("\n");
        html.append("      function validateModal() {\n");
        html.append("        const reasonOk = modalReason.value.trim().length >= 5;\n");
        html.append("        const valueOk = pendingDraft && pendingDraft.kind === 'tuneThreshold'\n");
        html.append("          ? Number(modalNewValue.value) > 0 : true;\n");
        html.append("        modalSubmit.disabled = !(reasonOk && valueOk);\n");
        html.append("      }\n");
        html.append("\n");
        html.append("      modalReason.addEventListener('input', validateModal);\n");
        html.append("      modalNewValue.addEventListener('input', validateModal);\n");
        html.append("      modalForm.querySelector('.cancel-btn').addEventListener('click', closeModal);\n");
        html.append("      modalForm.addEventListener('submit', e => {\n");
        html.append("        e.preventDefault();\n");
        html.append("        if (!pendingDraft) return;\n");
        html.append("        const change = { kind: pendingDraft.kind, params: pendingDraft.params, reason: modalReason.value.trim() };\n");
        html.append("        if (pendingDraft.kind === 'tuneThreshold') {\n");
        html.append("          change.params.newValue = String(Number(modalNewValue.value));\n");
        html.append("        }\n");
        html.append("        pending.push(change);\n");
        html.append("        localStorage.setItem(STORAGE_KEY, JSON.stringify(pending));\n");
        html.append("        closeModal();\n");
        html.append("        render();\n");
        html.append("      });\n");
        html.append("\n");
        html.append("      document.addEventListener('click', e => {\n");
        html.append("        const suppress = e.target.closest('.suppress-btn');\n");
        html.append("        if (suppress) {\n");
        html.append("          openModal({ kind: 'suppressFinding', title: 'Suppress finding',\n");
        html.append("            summary: 'Adds <code>@SuppressWarnings(\"CleanCode:'+suppress.dataset.code+'\")</code> in ' + esc(suppress.dataset.file) + ':' + suppress.dataset.line,\n");
        html.append("            params: { code: suppress.dataset.code, file: suppress.dataset.file, line: suppress.dataset.line } });\n");
        html.append("          return;\n");
        html.append("        }\n");
        html.append("        const disable = e.target.closest('.disable-btn');\n");
        html.append("        if (disable) {\n");
        html.append("          openModal({ kind: 'disableRecipe', title: 'Disable rule project-wide',\n");
        html.append("            summary: 'Adds <code>\"'+disable.dataset.code+'\"</code> to <code>cleanCode.disabledRecipes</code>',\n");
        html.append("            params: { code: disable.dataset.code } });\n");
        html.append("          return;\n");
        html.append("        }\n");
        html.append("        const tune = e.target.closest('.tune-btn');\n");
        html.append("        if (tune) {\n");
        html.append("          const key = tune.dataset.threshold;\n");
        html.append("          const current = serverState && serverState.thresholds ? serverState.thresholds[key] : '?';\n");
        html.append("          openModal({ kind: 'tuneThreshold', title: 'Tune threshold',\n");
        html.append("            summary: 'Updates <code>cleanCode.thresholds.'+key+'</code> in build.gradle.kts',\n");
        html.append("            params: { code: tune.dataset.code, threshold: key },\n");
        html.append("            currentValue: current, suggestedValue: typeof current === 'number' ? current + 2 : 1 });\n");
        html.append("          return;\n");
        html.append("        }\n");
        html.append("        const fix = e.target.closest('.fix-btn');\n");
        html.append("        if (fix) {\n");
        html.append("          openModal({ kind: 'applyRefactoring', title: 'Apply fix recipe',\n");
        html.append("            summary: 'Runs the registered recipe(s) for <code>'+fix.dataset.code+'</code> against ' + esc(fix.dataset.file),\n");
        html.append("            params: { code: fix.dataset.code, file: fix.dataset.file, line: fix.dataset.line } });\n");
        html.append("          return;\n");
        html.append("        }\n");
        html.append("        const fixAll = e.target.closest('.fix-all-btn');\n");
        html.append("        if (fixAll) {\n");
        html.append("          openModal({ kind: 'applyRefactoring', title: 'Apply fix across project',\n");
        html.append("            summary: 'Runs the registered recipe(s) for <code>'+fixAll.dataset.code+'</code> against every .java file in the project',\n");
        html.append("            params: { code: fixAll.dataset.code, file: '' } });\n");
        html.append("          return;\n");
        html.append("        }\n");
        html.append("      });\n");
        html.append("\n");
        html.append("      stopBtn.addEventListener('click', () => {\n");
        html.append("        if (!confirm('Stop the cleanCodeServe task? You will need to re-run ./gradlew cleanCodeServe to resume.')) return;\n");
        html.append("        stopBtn.disabled = true;\n");
        html.append("        stopBtn.textContent = 'Stopping...';\n");
        html.append("        fetch('/api/shutdown', { method: 'POST' })\n");
        html.append("          .then(() => setServerStopped())\n");
        html.append("          .catch(() => setServerStopped());\n");
        html.append("      });\n");
        html.append("\n");
        html.append("      discardBtn.addEventListener('click', () => {\n");
        html.append("        if (pending.length === 0) return;\n");
        html.append("        if (!confirm('Discard ' + pending.length + ' pending change(s)?')) return;\n");
        html.append("        pending = [];\n");
        html.append("        localStorage.removeItem(STORAGE_KEY);\n");
        html.append("        render();\n");
        html.append("      });\n");
        html.append("\n");
        html.append("      confirmBtn.addEventListener('click', () => {\n");
        html.append("        if (pending.length === 0) return;\n");
        html.append("        confirmBtn.disabled = true;\n");
        html.append("        confirmBtn.textContent = 'Applying...';\n");
        html.append("        fetch('/api/apply-changes', {\n");
        html.append("          method: 'POST',\n");
        html.append("          headers: { 'Content-Type': 'application/json' },\n");
        html.append("          body: JSON.stringify({ changes: pending })\n");
        html.append("        }).then(r => r.json().then(j => ({ status: r.status, body: j })))\n");
        html.append("          .then(({ status, body }) => {\n");
        html.append("            if (status === 200 && body.success) {\n");
        html.append("              pending = [];\n");
        html.append("              localStorage.removeItem(STORAGE_KEY);\n");
        html.append("              location.reload();\n");
        html.append("            } else {\n");
        html.append("              const errs = (body.errors || ['unknown error']).join('\\n');\n");
        html.append("              alert('Apply failed:\\n' + errs);\n");
        html.append("              confirmBtn.disabled = false;\n");
        html.append("              confirmBtn.textContent = 'Confirm & apply';\n");
        html.append("            }\n");
        html.append("          }).catch(err => {\n");
        html.append("            alert('Network error: ' + err.message);\n");
        html.append("            confirmBtn.disabled = false;\n");
        html.append("            confirmBtn.textContent = 'Confirm & apply';\n");
        html.append("          });\n");
        html.append("      });\n");
        html.append("\n");
        html.append("      load();\n");
        html.append("    })();\n");
        html.append("  </script>\n");
    }

    private static void appendSeveritySummary(StringBuilder html, AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        final int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        final int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        final int info = bySeverity.getOrDefault(Severity.INFO, List.of()).size();

        html.append("    <div class=\"summary\" id=\"severity-filter\" ");
        html.append("title=\"Click a badge to hide findings of that severity\">\n");
        html.append("      <button type=\"button\" class=\"badge error\" data-severity-toggle=\"error\">");
        html.append(errors).append(" errors</button>\n");
        html.append("      <button type=\"button\" class=\"badge warning\" data-severity-toggle=\"warning\">");
        html.append(warnings).append(" warnings</button>\n");
        html.append("      <button type=\"button\" class=\"badge info\" data-severity-toggle=\"info\">");
        html.append(info).append(" info</button>\n");
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

        html.append("    <details data-code=\"").append(escape(code.name())).append("\">\n");
        html.append("      <summary><span class=\"code-label\">").append(escape(code.name()));
        html.append("</span>").append(escape(name));
        html.append(" (").append(group.size()).append(")");
        appendCodeActions(html, code);
        html.append("</summary>\n");
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
        html.append("<col class=\"severity\"><col class=\"confidence\">");
        html.append("<col class=\"location\"><col class=\"message\"><col class=\"actions\">");
        html.append("</colgroup>\n");
        html.append("          <tr><th>Severity</th><th>Confidence</th>");
        html.append("<th>Location</th><th>Message</th><th></th></tr>\n");

        group.stream()
                .sorted(Comparator.comparing(f -> f.sourceFile() != null ? f.sourceFile() : ""))
                .forEach(f -> appendFindingRow(html, f, code, repositoryUrl, projectRoot, ideUrlScheme));

        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </details>\n");
    }

    private static void appendFindingRow(StringBuilder html, Finding finding,
                                          HeuristicCode code,
                                          String repositoryUrl, Path projectRoot,
                                          String ideUrlScheme) {
        final String severityLevel = finding.severity().name().toLowerCase();
        final String severityClass = "sev-" + severityLevel;
        final String confidenceLevel = finding.confidence().name().toLowerCase();
        final String location = formatLocation(finding);
        final String locationHtml = buildLocationHtml(finding, location, repositoryUrl,
                projectRoot, ideUrlScheme);
        final java.util.Optional<SnippetReader.Snippet> snippet = SnippetReader.read(finding, projectRoot);

        html.append("          <tr data-severity=\"").append(severityLevel)
                .append("\" data-confidence=\"").append(confidenceLevel).append("\">");
        html.append("<td class=\"").append(severityClass).append("\">");
        html.append(finding.severity().name()).append("</td>");
        html.append("<td><span class=\"confidence-pill ").append(confidenceLevel).append("\">");
        html.append(finding.confidence().name()).append("</span></td>");
        html.append("<td class=\"location\">").append(locationHtml).append("</td>");
        html.append("<td>").append(escape(finding.message()));
        if (snippet.isPresent()) {
            html.append("<span class=\"snippet-toggle\" data-action=\"toggle-snippet\">");
            html.append("&#9656; snippet</span>");
        }
        html.append("</td>");
        html.append("<td class=\"actions\">");
        html.append(buildFixButton(finding, code));
        html.append(buildSuppressButton(finding, code));
        html.append("</td>");
        html.append("</tr>\n");

        if (snippet.isPresent()) {
            appendSnippetRow(html, snippet.get(), severityLevel, confidenceLevel);
        }
    }

    private static String buildFixButton(final Finding finding, final HeuristicCode code) {
        if (!RefactoringRegistry.hasRecipeFor(code)) {
            return "";
        }
        if (finding.sourceFile() == null || finding.startLine() <= 0) {
            return "";
        }
        return "<button type=\"button\" class=\"action-btn fix-btn\""
                + " data-code=\"" + escape(code.name()) + "\""
                + " data-file=\"" + escape(finding.sourceFile()) + "\""
                + " data-line=\"" + finding.startLine() + "\""
                + " title=\"Apply the registered refactoring recipe to this file\""
                + ">&#128295; Fix</button>";
    }

    private static void appendSnippetRow(final StringBuilder html, final SnippetReader.Snippet snippet,
                                          final String severityLevel, final String confidenceLevel) {
        html.append("          <tr class=\"snippet-row\" data-severity=\"").append(severityLevel);
        html.append("\" data-confidence=\"").append(confidenceLevel);
        html.append("\" hidden><td colspan=\"5\"><pre>");
        for (int i = 0; i < snippet.lines().size(); i++) {
            final int lineNumber = snippet.firstLineNumber() + i;
            final boolean focal = lineNumber >= snippet.focalStartLine() && lineNumber <= snippet.focalEndLine();
            // Every snippet line is its own CSS block; we never emit
            // literal newlines between rows because <pre> would render
            // them as extra blank lines on top of the block layout.
            html.append("<span class=\"ln-row").append(focal ? " focal" : "").append("\">");
            html.append("<span class=\"ln\">").append(lineNumber).append("</span>");
            final String content = snippet.lines().get(i);
            html.append(content.isEmpty() ? "&#8203;" : escape(content));
            html.append("</span>");
        }
        html.append("</pre></td></tr>\n");
    }

    private static String buildSuppressButton(final Finding finding, final HeuristicCode code) {
        if (finding.sourceFile() == null || finding.startLine() <= 0) {
            return "";
        }
        return "<button type=\"button\" class=\"action-btn suppress-btn\""
                + " data-code=\"" + escape(code.name()) + "\""
                + " data-file=\"" + escape(finding.sourceFile()) + "\""
                + " data-line=\"" + finding.startLine() + "\""
                + " title=\"Suppress this specific finding with @SuppressWarnings\""
                + ">&#128264; Suppress</button>";
    }

    private static void appendCodeActions(final StringBuilder html, final HeuristicCode code) {
        html.append("<span class=\"code-actions\">");
        if (RefactoringRegistry.hasRecipeFor(code)) {
            html.append("<button type=\"button\" class=\"action-btn fix-all-btn\"")
                    .append(" data-code=\"").append(escape(code.name())).append("\"")
                    .append(" title=\"Apply the registered refactoring recipe across every file in this project\"")
                    .append(">&#128295; Fix all</button>");
        }
        html.append("<button type=\"button\" class=\"action-btn disable-btn\"")
                .append(" data-code=\"").append(escape(code.name())).append("\"")
                .append(" title=\"Disable this rule project-wide\"")
                .append(">&#10060; Disable</button>");
        final String thresholdKey = thresholdKeyFor(code);
        if (thresholdKey != null) {
            html.append("<button type=\"button\" class=\"action-btn tune-btn\"")
                    .append(" data-code=\"").append(escape(code.name())).append("\"")
                    .append(" data-threshold=\"").append(escape(thresholdKey)).append("\"")
                    .append(" title=\"Raise the threshold to silence borderline cases\"")
                    .append(">&#9881;&#65039; Tune</button>");
        }
        html.append("</span>");
    }

    private static String thresholdKeyFor(final HeuristicCode code) {
        return switch (code) {
            case G30 -> "methodBlankLineSections";
            case T1 -> "privateMethodMinLines";
            case G25 -> "magicStringMinOccurrences";
            case G35 -> "hardcodedListMinLiterals";
            case Ch10_1 -> "classLineCount";
            case F1 -> "recordComponentCount";
            case G5 -> "cpdMinimumTokens";
            default -> null;
        };
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

    private static void appendToolSummary(StringBuilder html, List<Finding> findings, List<SourceState> sourceStates) {
        html.append("    <div class=\"tool-summary\">\n");
        html.append("      <h2>Sources</h2>\n");
        html.append("      <table>\n");
        html.append("        <tr><th>Tool</th><th>Status</th></tr>\n");

        if (sourceStates.isEmpty()) {
            final Map<String, Long> byTool = findings.stream()
                    .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));
            byTool.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        html.append("        <tr><td>").append(escape(entry.getKey()));
                        html.append("</td><td>").append(entry.getValue()).append("</td></tr>\n");
                    });
        } else {
            sourceStates.stream()
                    .sorted(Comparator.comparingInt((SourceState s) -> -s.findingCount())
                            .thenComparing(SourceState::displayName))
                    .forEach(state -> {
                        html.append("        <tr><td>").append(escape(state.displayName()));
                        html.append("</td><td>").append(escape(formatSourceStatus(state))).append("</td></tr>\n");
                    });
        }

        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private static String formatSourceStatus(final SourceState state) {
        return switch (state.status()) {
            case PRODUCED_FINDINGS -> state.findingCount() + " findings";
            case RAN_NO_FINDINGS -> "ran, no findings";
            case NOT_AVAILABLE -> "not available — report not found";
        };
    }

    private static void appendOptionalRulesSummary(StringBuilder html) {
        if (OptionalRules.defaults().isEmpty()) {
            return;
        }
        html.append("    <div class=\"optional-rules\">\n");
        html.append("      <h2>Optional rules</h2>\n");
        html.append("      <p>Disabled by default. To enable, add to ");
        html.append("<code>cleanCode.enabledOptionalRules</code> in your build script.</p>\n");
        html.append("      <table>\n");
        html.append("        <tr><th>Rule</th><th>Code</th><th>What it enforces</th></tr>\n");
        OptionalRules.defaults().values().forEach(rule -> {
            html.append("        <tr><td><code>").append(escape(rule.key())).append("</code></td>");
            html.append("<td>").append(escape(rule.code())).append("</td>");
            html.append("<td>").append(escape(rule.summary())).append("</td></tr>\n");
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
