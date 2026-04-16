package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.nio.file.Path;

public class CleanCodePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final CleanCodeExtension ext = project.getExtensions()
                .create("cleanCode", CleanCodeExtension.class);

        new StaticAnalysisPluginConfigurer(project, ext).apply();

        project.afterEvaluate(p -> {
            final Path skillsDir = p.getProjectDir().toPath().resolve(ext.getSkillsDir().get());
            new SkillFileScaffolder(skillsDir, ext.getThresholds(), p.getLogger()).scaffold();
        });

        new CleanCodeTaskRegistrar(project).registerAll();
    }
}
