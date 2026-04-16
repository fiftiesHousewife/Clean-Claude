package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

final class CleanCodeTaskRegistrar {

    private static final String DEPENDENCY_UPDATES_TASK = "dependencyUpdates";
    private static final String VERIFICATION_GROUP = "verification";

    private final Project project;

    CleanCodeTaskRegistrar(final Project project) {
        this.project = project;
    }

    void registerAll() {
        final TaskProvider<AnalyseTask> analyse = project.getTasks()
                .register("analyseCleanCode", AnalyseTask.class, task -> {
                    task.setDescription("Analyse codebase for Clean Code violations");
                    task.setGroup(VERIFICATION_GROUP);
                });

        wireAnalyseDependencies(analyse);

        project.getTasks()
                .register("generateClaudeMd", GenerateClaudeMdTask.class, task -> {
                    task.setDescription("Generate CLAUDE.md from analysis findings");
                    task.setGroup(VERIFICATION_GROUP);
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeBaseline", BaselineTask.class, task -> {
                    task.setDescription("Snapshot current findings as baseline");
                    task.setGroup(VERIFICATION_GROUP);
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeFixPlan", FixPlanTask.class, task -> {
                    task.setDescription("Group findings by file into per-class fix briefs for agent handoff");
                    task.setGroup(VERIFICATION_GROUP);
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeExplain", ExplainTask.class, task -> {
                    task.setDescription("Print skill guidance for a finding concern");
                    task.setGroup("help");
                });
    }

    private void wireAnalyseDependencies(final TaskProvider<AnalyseTask> analyse) {
        project.afterEvaluate(p -> analyse.configure(task -> {
            task.dependsOn("pmdMain", "checkstyleMain", "jacocoTestReport",
                    "spotbugsMain", "cpdMain");

            if (p.getTasks().findByName(DEPENDENCY_UPDATES_TASK) != null) {
                task.dependsOn(DEPENDENCY_UPDATES_TASK);
            }
        }));
    }
}
