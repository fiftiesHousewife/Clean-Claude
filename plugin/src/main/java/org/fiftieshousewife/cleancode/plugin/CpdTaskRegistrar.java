package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;

import java.io.IOException;
import java.nio.file.Files;

final class CpdTaskRegistrar {

    private static final String CPD_VERSION = "7.9.0";

    private final Project project;
    private final Property<Integer> minimumTokens;

    CpdTaskRegistrar(final Project project, final Property<Integer> minimumTokens) {
        this.project = project;
        this.minimumTokens = minimumTokens;
    }

    void register() {
        final var cpdConfig = project.getConfigurations().create("cpd", conf -> {
            conf.setDescription("CPD classpath");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
        });

        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-cli:" + CPD_VERSION);
        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-java:" + CPD_VERSION);

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
                    throw new org.gradle.api.GradleException("Cannot open CPD report file", e);
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
