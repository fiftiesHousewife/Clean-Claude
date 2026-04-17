package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CpdTaskRegistrar {

    private static final String CPD_VERSION = "7.9.0";
    private static final String CPD_CONFIGURATION = "cpd";
    private static final String CPD_REPORT_PATH = "reports/cpd/cpd.xml";
    private static final String CPD_TASK_NAME = "cpdMain";

    private final Project project;
    private final Property<Integer> minimumTokens;

    CpdTaskRegistrar(final Project project, final Property<Integer> minimumTokens) {
        this.project = project;
        this.minimumTokens = minimumTokens;
    }

    void register() {
        createCpdConfiguration();
        addCpdDependencies();
        registerCpdTask();
    }

    private Configuration createCpdConfiguration() {
        return project.getConfigurations().create(CPD_CONFIGURATION, conf -> {
            conf.setDescription("CPD classpath");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
        });
    }

    private void addCpdDependencies() {
        project.getDependencies().add(CPD_CONFIGURATION, "net.sourceforge.pmd:pmd-cli:" + CPD_VERSION);
        project.getDependencies().add(CPD_CONFIGURATION, "net.sourceforge.pmd:pmd-java:" + CPD_VERSION);
    }

    private void registerCpdTask() {
        project.getTasks().register(CPD_TASK_NAME, JavaExec.class, this::configureCpdTask);
    }

    private void configureCpdTask(final JavaExec task) {
        final Configuration cpdConfig = project.getConfigurations().getByName(CPD_CONFIGURATION);
        final Provider<RegularFile> reportFile = project.getLayout().getBuildDirectory().file(CPD_REPORT_PATH);

        task.setDescription("Run CPD copy-paste detection");
        task.setGroup("verification");
        task.classpath(cpdConfig);
        task.getMainClass().set("net.sourceforge.pmd.cli.PmdCli");
        task.setIgnoreExitValue(true);

        task.doFirst(t -> redirectStandardOutputToReport(task, reportFile));

        task.args(
                "cpd",
                "--minimum-tokens", String.valueOf(minimumTokens.get()),
                "--language", "java",
                "--format", "xml",
                "--dir", project.file("src/main/java").getAbsolutePath());
    }

    private static void redirectStandardOutputToReport(
            final JavaExec task, final Provider<RegularFile> reportFile) {
        final Path reportPath = reportFile.get().getAsFile().toPath();
        createParentDirectories(reportPath);
        try {
            task.setStandardOutput(Files.newOutputStream(reportPath));
        } catch (final IOException cause) {
            throw new GradleException("Cannot open CPD report file", cause);
        }
    }

    private static void createParentDirectories(final Path reportPath) {
        final Path parent = reportPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (final IOException cause) {
            throw new GradleException("Cannot create CPD report directory", cause);
        }
    }
}
