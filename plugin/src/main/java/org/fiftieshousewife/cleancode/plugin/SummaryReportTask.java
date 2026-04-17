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
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SummaryReportTask extends DefaultTask {

    @TaskAction
    public void run() throws IOException {
        final Project rootProject = getProject().getRootProject();
        final Path rootDir = rootProject.getProjectDir().toPath();
        final Path summary = rootDir.resolve("docs/reports/index.html");
        Files.createDirectories(summary.getParent());

        final Map<String, SummaryCounts> byModule = new LinkedHashMap<>();
        byModule.put("(root)", readCountsFor(
                rootProject.getLayout().getBuildDirectory().get().getAsFile().toPath()));
        for (final Project sub : rootProject.getSubprojects()) {
            byModule.put(sub.getName(), readCountsFor(
                    sub.getLayout().getBuildDirectory().get().getAsFile().toPath()));
        }

        SummaryCounts totals = SummaryCounts.zero();
        for (final SummaryCounts c : byModule.values()) {
            totals = totals.plus(c);
        }

        Files.writeString(summary, new SummaryReportHtml(byModule, totals).render());
        getLogger().lifecycle("Wrote summary report: {}", summary);
    }

    private SummaryCounts readCountsFor(final Path buildDir) {
        final Path findings = buildDir.resolve("reports/clean-code/findings.json");
        if (!Files.exists(findings)) {
            return SummaryCounts.zero();
        }
        try (Reader reader = Files.newBufferedReader(findings)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            final JsonArray array = root.getAsJsonArray("findings");
            if (array == null) {
                return SummaryCounts.zero();
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
            return new SummaryCounts(errors, warnings, info);
        } catch (IOException e) {
            return SummaryCounts.zero();
        }
    }
}
