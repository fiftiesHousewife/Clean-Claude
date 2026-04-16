package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;

import java.nio.file.Path;

final class ProjectReportPaths {

    private static final String FINDINGS_REPORT_PATH = "reports/clean-code/findings.json";

    private ProjectReportPaths() {
    }

    static Path buildDir(final Project project) {
        return project.getLayout().getBuildDirectory().get().getAsFile().toPath();
    }

    static Path findingsReport(final Project project) {
        return buildDir(project).resolve(FINDINGS_REPORT_PATH);
    }
}
