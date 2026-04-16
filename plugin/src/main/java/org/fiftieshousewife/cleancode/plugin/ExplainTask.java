package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.SkillPathRegistry;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public abstract class ExplainTask extends DefaultTask {

    private String finding = "";

    @Option(option = "finding", description = "The finding concern to explain (e.g. error-handling)")
    public void setFinding(String finding) {
        this.finding = finding;
    }

    @TaskAction
    public void explain() throws IOException {
        final String skillPath = resolveSkillPath(finding);
        if (skillPath == null) {
            getLogger().lifecycle("No skill file found for: {}", finding);
            return;
        }

        final Path file = getProject().getProjectDir().toPath().resolve(skillPath);
        if (!Files.exists(file)) {
            getLogger().lifecycle("Skill file not found: {}", skillPath);
            return;
        }

        getLogger().lifecycle(Files.readString(file));
    }

    private String resolveSkillPath(String concern) {
        return switch (concern.toLowerCase(Locale.ROOT)) {
            case "error-handling", "exceptions" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_1);
            case "null-handling", "nulls" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_2);
            case "class-structure", "classes", "srp" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch10_1);
            case "functions", "methods" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch3_1);
            default -> null;
        };
    }
}
