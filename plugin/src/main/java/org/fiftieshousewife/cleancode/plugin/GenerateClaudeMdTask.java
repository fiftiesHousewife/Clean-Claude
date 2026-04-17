package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.ClaudeMdGenerator;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public abstract class GenerateClaudeMdTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final TaskPaths paths = TaskPaths.of(getProject());
        final List<String> dependencies = RuntimeDependencies.groupAndName(getProject());

        final AggregatedReport report = JsonReportReader.read(paths.findingsReport());
        ClaudeMdGenerator.generate(report, paths.projectFile("CLAUDE.md"), paths.baselineFile(), dependencies);

        getLogger().lifecycle("Generated CLAUDE.md with {} findings", report.findings().size());
    }
}
