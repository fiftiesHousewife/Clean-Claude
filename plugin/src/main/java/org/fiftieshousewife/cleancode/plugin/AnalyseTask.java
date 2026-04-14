package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.adapters.*;
import org.fiftieshousewife.cleancode.core.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.List;

public abstract class AnalyseTask extends DefaultTask {

    @TaskAction
    public void analyse() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportsDir = buildDir.resolve("reports");

        final ProjectContext context = new ProjectContext(
                projectRoot,
                getProject().getName(),
                getProject().getVersion().toString(),
                "21",
                List.of(projectRoot.resolve("src/main/java")),
                List.of(projectRoot.resolve("src/test/java")),
                buildDir,
                reportsDir);

        final List<FindingSource> sources = List.of(
                new PmdFindingSource(),
                new CheckstyleFindingSource(),
                new SpotBugsFindingSource(),
                new CpdFindingSource(),
                new JacocoFindingSource(),
                new SurefireFindingSource(),
                new DependencyUpdatesFindingSource(),
                new OpenRewriteFindingSource());

        final AggregatedReport report = FindingAggregator.aggregate(sources, context);

        final Path outputDir = buildDir.resolve("reports/clean-code");
        JsonReportWriter.write(report, outputDir.resolve("findings.json"));

        getLogger().lifecycle(BuildOutputFormatter.format(report));
    }
}
