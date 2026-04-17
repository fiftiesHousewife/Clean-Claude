package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;

import java.nio.file.Path;

final class TaskPaths {

    private static final String FINDINGS_JSON = "reports/clean-code/findings.json";
    private static final String BASELINE_JSON = "clean-code-baseline.json";

    private final Path buildDir;
    private final Path projectDir;

    private TaskPaths(final Path buildDir, final Path projectDir) {
        this.buildDir = buildDir;
        this.projectDir = projectDir;
    }

    static TaskPaths of(final Project project) {
        return new TaskPaths(
                project.getLayout().getBuildDirectory().get().getAsFile().toPath(),
                project.getProjectDir().toPath());
    }

    Path findingsReport() {
        return buildDir.resolve(FINDINGS_JSON);
    }

    Path baselineFile() {
        return projectDir.resolve(BASELINE_JSON);
    }

    Path projectFile(final String name) {
        return projectDir.resolve(name);
    }
}
