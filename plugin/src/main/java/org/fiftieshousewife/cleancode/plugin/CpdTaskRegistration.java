package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;

import java.io.IOException;
import java.nio.file.Files;

final class CpdTaskRegistration {

    private static final String CPD_CONFIGURATION = "cpd";
    private static final String PMD_VERSION = "7.9.0";

    private final Project project;
    private final CleanCodeExtension extension;

    CpdTaskRegistration(final Project project, final CleanCodeExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    void register() {
        final var cpdConfig = project.getConfigurations().create(CPD_CONFIGURATION, conf -> {
            conf.setDescription("CPD classpath");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
        });

        project.getDependencies().add(CPD_CONFIGURATION, "net.sourceforge.pmd:pmd-cli:" + PMD_VERSION);
        project.getDependencies().add(CPD_CONFIGURATION, "net.sourceforge.pmd:pmd-java:" + PMD_VERSION);

        final var minimumTokens = extension.getThresholds().getCpdMinimumTokens();

        project.getTasks().register("cpdMain", JavaExec.class, task -> {
            task.setDescription("Run CPD copy-paste detection");
            task.setGroup("verification");
            task.classpath(cpdConfig);
            task.getMainClass().set("net.sourceforge.pmd.cli.PmdCli");
            task.setIgnoreExitValue(true);

            final var reportFile = project.getLayout().getBuildDirectory()
                    .file("reports/cpd/cpd.xml");

            task.doFirst(t -> {
                reportFile.get().getAsFile().getParentFile().mkdirs();
                try {
                    task.setStandardOutput(
                            Files.newOutputStream(reportFile.get().getAsFile().toPath()));
                } catch (IOException e) {
                    throw new GradleException("Cannot open CPD report file", e);
                }
            });

            task.args(
                    "cpd",
                    "--minimum-tokens", String.valueOf(minimumTokens.get()),
                    "--language", "java",
                    "--format", "xml",
                    "--dir", project.file("src/main/java").getAbsolutePath());
        });
    }
}
