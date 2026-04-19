package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.ClaudeMdGenerator;
import io.github.fiftieshousewife.cleancode.core.JsonReportReader;
import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.nio.file.Path;
import java.util.List;

@DisableCachingByDefault(because = "writes CLAUDE.md using findings.json and the baseline; reads via convention paths resolved at runtime")
public abstract class GenerateClaudeMdTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportFile = buildDir.resolve("reports/clean-code/findings.json");
        final Path claudeMdFile = getProject().getProjectDir().toPath().resolve("CLAUDE.md");
        final Path baselineFile = getProject().getProjectDir().toPath().resolve("clean-code-baseline.json");

        final List<String> dependencies = getProject().getConfigurations().stream()
                .filter(c -> "runtimeClasspath".equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();

        final AggregatedReport report = JsonReportReader.read(reportFile);
        ClaudeMdGenerator.generate(report, claudeMdFile, baselineFile, dependencies);

        getLogger().lifecycle("Generated CLAUDE.md with {} findings", report.findings().size());
    }
}
