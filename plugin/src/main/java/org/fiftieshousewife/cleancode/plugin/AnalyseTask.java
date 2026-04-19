package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.BaselineManager;
import org.fiftieshousewife.cleancode.core.BuildOutputFormatter;
import org.fiftieshousewife.cleancode.core.HtmlReportWriter;
import org.fiftieshousewife.cleancode.core.JsonReportWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public abstract class AnalyseTask extends DefaultTask {

    @TaskAction
    public void analyse() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final AggregatedReport report = SandboxAnalysis.analyse(getProject());

        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final String baseRepoUrl = ext.getRepositoryUrl().get();
        final String modulePath = getProject().getRootDir().toPath().relativize(projectRoot).toString();
        final String repositoryUrl = baseRepoUrl.isBlank() ? ""
                : baseRepoUrl + "/blob/main" + (modulePath.isEmpty() ? "" : "/" + modulePath);
        final Path outputDir = buildDir.resolve("reports/clean-code");
        final Path htmlReport = outputDir.resolve("findings.html");
        JsonReportWriter.write(report, outputDir.resolve("findings.json"));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl);

        final Path baselineFile = projectRoot.resolve("clean-code-baseline.json");
        final Map<HeuristicCode, BaselineManager.Delta> deltas = Files.exists(baselineFile)
                ? BaselineManager.computeDeltas(report, baselineFile)
                : Map.of();

        getLogger().lifecycle(BuildOutputFormatter.format(report, deltas));
        getLogger().lifecycle("\n  Report: file://" + htmlReport.toAbsolutePath());
    }
}
