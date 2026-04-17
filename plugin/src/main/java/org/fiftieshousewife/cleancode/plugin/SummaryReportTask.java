package org.fiftieshousewife.cleancode.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SummaryReportTask extends DefaultTask {

    private record Counts(int errors, int warnings, int info) {
        Counts plus(Counts other) {
            return new Counts(errors + other.errors, warnings + other.warnings, info + other.info);
        }

        int total() {
            return errors + warnings + info;
        }
    }

    @TaskAction
    public void run() throws IOException {
        final Project rootProject = getProject().getRootProject();
        final Path rootDir = rootProject.getProjectDir().toPath();
        final Path summary = rootDir.resolve("docs/reports/index.html");
        Files.createDirectories(summary.getParent());

        final Map<String, Counts> byModule = new LinkedHashMap<>();
        byModule.put("(root)", readCountsFor(rootDir,
                rootProject.getLayout().getBuildDirectory().get().getAsFile().toPath()));
        for (final Project sub : rootProject.getSubprojects()) {
            byModule.put(sub.getName(), readCountsFor(rootDir,
                    sub.getLayout().getBuildDirectory().get().getAsFile().toPath()));
        }

        Counts totals = new Counts(0, 0, 0);
        for (final Counts c : byModule.values()) {
            totals = totals.plus(c);
        }

        Files.writeString(summary, renderHtml(byModule, totals));
        getLogger().lifecycle("Wrote summary report: {}", summary);
    }

    private Counts readCountsFor(final Path rootDir, final Path buildDir) {
        final Path findings = buildDir.resolve("reports/clean-code/findings.json");
        if (!Files.exists(findings)) {
            return new Counts(0, 0, 0);
        }
        try (Reader reader = Files.newBufferedReader(findings)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            final JsonArray array = root.getAsJsonArray("findings");
            if (array == null) {
                return new Counts(0, 0, 0);
            }
            int errors = 0;
            int warnings = 0;
            int info = 0;
            for (final JsonElement element : array) {
                final String severity = element.getAsJsonObject().get("severity").getAsString();
                switch (severity) {
                    case "ERROR" -> errors++;
                    case "WARNING" -> warnings++;
                    case "INFO" -> info++;
                    default -> { }
                }
            }
            return new Counts(errors, warnings, info);
        } catch (IOException e) {
            return new Counts(0, 0, 0);
        }
    }

    private String renderHtml(final Map<String, Counts> byModule, final Counts totals) {
        final StringBuilder sb = new StringBuilder();
        renderHead(sb);
        renderHeader(sb);
        renderTable(sb, byModule, totals);
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

    private void renderTable(final StringBuilder sb, final Map<String, Counts> byModule, final Counts totals) {
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
