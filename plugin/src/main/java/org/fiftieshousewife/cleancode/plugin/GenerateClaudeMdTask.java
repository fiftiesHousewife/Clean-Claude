package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.ClaudeMdGenerator;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.List;

public abstract class GenerateClaudeMdTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final Path reportFile = ProjectReportPaths.findingsReport(getProject());
        final Path projectDir = getProject().getProjectDir().toPath();
        final Path claudeMdFile = projectDir.resolve("CLAUDE.md");
        final Path baselineFile = projectDir.resolve("clean-code-baseline.json");

        final List<String> dependencies = resolveRuntimeDependencies();

        final AggregatedReport report = JsonReportReader.read(reportFile);
        ClaudeMdGenerator.generate(report, claudeMdFile, baselineFile, dependencies);

        getLogger().lifecycle("Generated CLAUDE.md with {} findings", report.findings().size());
    }

    private List<String> resolveRuntimeDependencies() {
        return getProject().getConfigurations().stream()
                .filter(c -> "runtimeClasspath".equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();
    }
}
