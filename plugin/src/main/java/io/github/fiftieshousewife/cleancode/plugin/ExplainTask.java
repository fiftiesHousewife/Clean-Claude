package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.AgentLayout;
import io.github.fiftieshousewife.cleancode.core.SkillPathRegistry;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "prints a skill-file lookup to stdout; pure side-effect with no cacheable output")
public abstract class ExplainTask extends DefaultTask {

    private String finding = "";

    @Option(option = "finding", description = "The finding concern to explain (e.g. error-handling)")
    public void setFinding(String finding) {
        this.finding = finding;
    }

    @TaskAction
    public void explain() throws IOException {
        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final AgentLayout layout = AgentLayout.resolve(getProject().getProjectDir().toPath(),
                null, ext.getSkillsDir().getOrElse(""));
        final String skillPath = resolveSkillPath(finding, layout.skillsDir());
        if (skillPath == null) {
            getLogger().lifecycle("No skill file found for: {}", finding);
            return;
        }

        Path file = getProject().getProjectDir().toPath().resolve(skillPath);
        if (!Files.exists(file)) {
            getLogger().lifecycle("Skill file not found: {}", skillPath);
            return;
        }

        getLogger().lifecycle(Files.readString(file));
    }

    private String resolveSkillPath(final String concern, final String skillsDir) {
        return switch (concern.toLowerCase()) {
            case "error-handling", "exceptions" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_1, skillsDir);
            case "null-handling", "nulls" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_2, skillsDir);
            case "class-structure", "classes", "srp" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch10_1, skillsDir);
            case "functions", "methods" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch3_1, skillsDir);
            default -> null;
        };
    }
}
