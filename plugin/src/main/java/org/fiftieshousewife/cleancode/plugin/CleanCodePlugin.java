package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.nio.file.Path;

public class CleanCodePlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final CleanCodeExtension extension = project.getExtensions()
                .create("cleanCode", CleanCodeExtension.class);

        new StaticAnalysisConfiguration(project, extension).apply();
        new CleanCodeTasks(project).register();
        scheduleSkillScaffolding(project, extension);
    }

    private void scheduleSkillScaffolding(final Project project, final CleanCodeExtension extension) {
        project.afterEvaluate(p -> {
            final Path skillsDir = p.getProjectDir().toPath().resolve(extension.getSkillsDir().get());
            new SkillFileScaffolder(skillsDir, extension.getThresholds(), p.getLogger()).scaffold();
        });
    }
}
