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
    public void setFinding(final String finding) {
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

    static String resolveSkillPath(final String concern, final String skillsDir) {
        final String aliasResolved = resolveAliasSkillPath(concern, skillsDir);
        if (aliasResolved != null) {
            return aliasResolved;
        }
        final HeuristicCode code = parseHeuristicCode(concern);
        return code == null ? null : SkillPathRegistry.skillPathFor(code, skillsDir);
    }

    private static String resolveAliasSkillPath(final String concern, final String skillsDir) {
        return switch (concern.toLowerCase()) {
            case "error-handling", "exceptions" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_1, skillsDir);
            case "null-handling", "nulls" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch7_2, skillsDir);
            case "class-structure", "classes", "srp" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch10_1, skillsDir);
            case "functions", "methods" -> SkillPathRegistry.skillPathFor(HeuristicCode.Ch3_1, skillsDir);
            default -> null;
        };
    }

    /**
     * Accepts {@code G18}, {@code Ch7.1}, {@code Ch7_1}, {@code J3}, etc.
     * — the codes as they appear in the build report and findings.json.
     * Hyphen and dot separators are normalised to underscore so users can
     * pass the form they see in the report.
     */
    private static HeuristicCode parseHeuristicCode(final String concern) {
        final String normalised = concern.trim().replace('.', '_').replace('-', '_');
        if (normalised.isEmpty()) {
            return null;
        }
        try {
            return HeuristicCode.valueOf(normalised);
        } catch (final IllegalArgumentException primary) {
            try {
                return HeuristicCode.valueOf(normalised.toUpperCase());
            } catch (final IllegalArgumentException secondary) {
                return null;
            }
        }
    }
}
