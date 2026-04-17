package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

final class CleanCodeTasks {

    private static final String DEPENDENCY_UPDATES_TASK = "dependencyUpdates";

    private final Project project;

    CleanCodeTasks(final Project project) {
        this.project = project;
    }

    void register() {
        final TaskProvider<AnalyseTask> analyse = registerAnalyse();
        wireTaskDependencies(analyse);
        registerGenerateClaudeMd(analyse);
        registerBaseline(analyse);
        registerFixPlan(analyse);
        registerExplain();

        if (project.getRootProject().equals(project)) {
            registerUpdateVersionCatalog();
            registerSummary();
        }
    }

    private TaskProvider<AnalyseTask> registerAnalyse() {
        return project.getTasks()
                .register("analyseCleanCode", AnalyseTask.class, task -> {
                    task.setDescription("Analyse codebase for Clean Code violations");
                    task.setGroup("verification");
                });
    }

    private void registerGenerateClaudeMd(final TaskProvider<AnalyseTask> analyse) {
        project.getTasks()
                .register("generateClaudeMd", GenerateClaudeMdTask.class, task -> {
                    task.setDescription("Generate CLAUDE.md from analysis findings");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });
    }

    private void registerBaseline(final TaskProvider<AnalyseTask> analyse) {
        project.getTasks()
                .register("cleanCodeBaseline", BaselineTask.class, task -> {
                    task.setDescription("Snapshot current findings as baseline");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });
    }

    private void registerFixPlan(final TaskProvider<AnalyseTask> analyse) {
        project.getTasks()
                .register("cleanCodeFixPlan", FixPlanTask.class, task -> {
                    task.setDescription("Group findings by file into per-class fix briefs for agent handoff");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });
    }

    private void registerExplain() {
        project.getTasks()
                .register("cleanCodeExplain", ExplainTask.class, task -> {
                    task.setDescription("Print skill guidance for a finding concern");
                    task.setGroup("help");
                });
    }

    private void registerUpdateVersionCatalog() {
        project.getTasks()
                .register("updateVersionCatalog", UpdateVersionCatalogTask.class, task -> {
                    task.setDescription(
                            "Rewrite gradle/libs.versions.toml with the non-major "
                                    + "updates from the dependencyUpdates report");
                    task.setGroup("verification");
                });
    }

    private void registerSummary() {
        project.getTasks()
                .register("cleanCodeSummary", SummaryReportTask.class, task -> {
                    task.setDescription(
                            "Aggregate every module's findings.json into "
                                    + "docs/reports/index.html with totals and links");
                    task.setGroup("verification");
                });
    }

    private void wireTaskDependencies(final TaskProvider<AnalyseTask> analyse) {
        project.afterEvaluate(p -> {
            analyse.configure(task -> {
                task.dependsOn("pmdMain", "checkstyleMain", "jacocoTestReport",
                        "spotbugsMain", "cpdMain");

                if (p.getTasks().findByName(DEPENDENCY_UPDATES_TASK) != null) {
                    task.dependsOn(DEPENDENCY_UPDATES_TASK);
                }
            });
        });
    }
}
