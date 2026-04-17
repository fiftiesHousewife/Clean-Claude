package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.FixBriefGenerator;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.List;

public abstract class FixPlanTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final TaskPaths paths = TaskPaths.of(getProject());
        final Path reportFile = paths.findingsReport();
        final Path outputDir = reportFile.getParent().resolve("fix-briefs");
        final Path projectRoot = getProject().getProjectDir().toPath();

        final AggregatedReport report = JsonReportReader.read(reportFile);
        final List<Path> written = FixBriefGenerator.generate(report, outputDir, projectRoot);

        getLogger().lifecycle("Wrote {} fix briefs to {}", written.size() - 1, outputDir);
    }
}
