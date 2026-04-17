package org.fiftieshousewife.cleancode.plugin;

import java.time.Instant;
import java.util.Map;

final class SummaryReportHtml {

    private final Map<String, SummaryCounts> byModule;
    private final SummaryCounts totals;

    SummaryReportHtml(final Map<String, SummaryCounts> byModule, final SummaryCounts totals) {
        this.byModule = byModule;
        this.totals = totals;
    }

    String render() {
        final StringBuilder sb = new StringBuilder();
        renderHead(sb);
        renderHeader(sb);
        renderTable(sb);
        renderFooterNote(sb);
        sb.append("</body></html>\n");
        return sb.toString();
    }

    private void renderHead(final StringBuilder sb) {
        sb.append("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<title>Clean Code summary</title><style>")
                .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;"
                        + "margin:0;padding:2rem;color:#222;background:#fafafa;}")
                .append("h1{margin:0 0 0.25rem;font-size:1.5rem;}")
                .append("p.meta{color:#666;margin:0 0 1.5rem;font-size:0.9rem;}")
                .append("table{border-collapse:collapse;min-width:680px;background:#fff;"
                        + "box-shadow:0 1px 3px rgba(0,0,0,0.08);}")
                .append("th,td{padding:0.6rem 1rem;border-bottom:1px solid #eee;text-align:left;}")
                .append("th{background:#1a1a2e;color:#fff;font-weight:600;}")
                .append("td.num{text-align:right;font-variant-numeric:tabular-nums;}")
                .append("tr.total td{font-weight:700;background:#f0f0f5;border-top:2px solid #1a1a2e;}")
                .append(".err{color:#c0392b;}")
                .append(".warn{color:#e67e22;}")
                .append(".info{color:#7f8c8d;}")
                .append("a{color:#1a73e8;text-decoration:none;}")
                .append("a:hover{text-decoration:underline;}")
                .append("</style></head><body>");
    }

    private void renderHeader(final StringBuilder sb) {
        sb.append("<h1>Clean Code — self-analysis summary</h1>");
        sb.append("<p class=\"meta\">Regenerated ").append(Instant.now())
                .append(" — aggregates every module's <code>findings.json</code> ")
                .append("and sums across the project.</p>");
    }

    private void renderTable(final StringBuilder sb) {
        sb.append("<table><thead><tr><th>Module</th>")
                .append("<th class=\"num err\">Errors</th>")
                .append("<th class=\"num warn\">Warnings</th>")
                .append("<th class=\"num info\">Info</th>")
                .append("<th class=\"num\">Total</th></tr></thead><tbody>");
        byModule.forEach((name, counts) -> sb.append("<tr>")
                .append("<td>")
                .append("<a href=\"").append(reportLink(name)).append("\">").append(escape(name)).append("</a>")
                .append("</td>")
                .append("<td class=\"num err\">").append(counts.errors()).append("</td>")
                .append("<td class=\"num warn\">").append(counts.warnings()).append("</td>")
                .append("<td class=\"num info\">").append(counts.info()).append("</td>")
                .append("<td class=\"num\">").append(counts.total()).append("</td>")
                .append("</tr>"));
        sb.append("<tr class=\"total\"><td>Total</td>")
                .append("<td class=\"num err\">").append(totals.errors()).append("</td>")
                .append("<td class=\"num warn\">").append(totals.warnings()).append("</td>")
                .append("<td class=\"num info\">").append(totals.info()).append("</td>")
                .append("<td class=\"num\">").append(totals.total()).append("</td>")
                .append("</tr>");
        sb.append("</tbody></table>");
    }

    private void renderFooterNote(final StringBuilder sb) {
        sb.append("<p class=\"meta\" style=\"margin-top:1.5rem\">")
                .append("E1 findings (outdated dependencies) are emitted only at the Gradle root — ")
                .append("sub-modules skip them once the catalog is anchored at ")
                .append("<code>gradle/libs.versions.toml</code>. ")
                .append("Counts are post-<code>@SuppressCleanCode</code>; raw pre-suppression baseline ")
                .append("(1,313) lives in <code>experiment/baseline/*.json</code>.")
                .append("</p>");
    }

    private String reportLink(final String name) {
        return "(root)".equals(name) ? "root.html" : name + ".html";
    }

    private String escape(final String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
