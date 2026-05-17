package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.AgentLayout;
import io.github.fiftieshousewife.cleancode.core.ClaudeMdGenerator;
import io.github.fiftieshousewife.cleancode.core.JsonReportReader;
import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.nio.file.Path;
import java.util.List;

@DisableCachingByDefault(because = "writes the agent's instructions file using findings.json and the baseline; reads via convention paths resolved at runtime")
public abstract class GenerateClaudeMdTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final Path projectDir = getProject().getProjectDir().toPath();
        final AgentLayout layout = AgentLayout.resolve(projectDir,
                ext.getAgentInstructionsFile().getOrElse(""),
                ext.getSkillsDir().getOrElse(""));

        final List<String> dependencies = getProject().getConfigurations().stream()
                .filter(c -> "runtimeClasspath".equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();

        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportFile = buildDir.resolve("reports/clean-code/findings.json");
        final AggregatedReport report = JsonReportReader.read(reportFile);

        final Path agentMdFile = projectDir.resolve(layout.instructionsFile());
        final Path baselineFile = projectDir.resolve("clean-code-baseline.json");
        ClaudeMdGenerator.generate(report, agentMdFile, baselineFile, dependencies, layout);

        getLogger().lifecycle("Generated {} with {} findings", layout.instructionsFile(), report.findings().size());
    }
}
