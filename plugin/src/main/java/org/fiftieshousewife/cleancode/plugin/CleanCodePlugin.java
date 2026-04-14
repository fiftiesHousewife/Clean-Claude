package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CleanCodePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        CleanCodeExtension ext = project.getExtensions()
                .create("cleanCode", CleanCodeExtension.class);

        var analyse = project.getTasks()
                .register("analyseCleanCode", AnalyseTask.class, task -> {
                    task.setDescription("Analyse codebase for Clean Code violations");
                    task.setGroup("verification");
                    task.mustRunAfter("check");
                });

        project.getTasks()
                .register("generateClaudeMd", GenerateClaudeMdTask.class, task -> {
                    task.setDescription("Generate CLAUDE.md from analysis findings");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeBaseline", BaselineTask.class, task -> {
                    task.setDescription("Snapshot current findings as baseline");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeExplain", ExplainTask.class, task -> {
                    task.setDescription("Print skill guidance for a finding concern");
                    task.setGroup("help");
                });
    }
}
