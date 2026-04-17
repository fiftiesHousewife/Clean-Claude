package org.fiftieshousewife.cleancode.plugin;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.github.spotbugs.snom.SpotBugsExtension;
import com.github.spotbugs.snom.SpotBugsPlugin;
import com.github.spotbugs.snom.SpotBugsTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.Pmd;
import org.gradle.api.plugins.quality.PmdExtension;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class StaticAnalysisConfiguration {

    private final Project project;
    private final CleanCodeExtension extension;

    StaticAnalysisConfiguration(final Project project, final CleanCodeExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    void apply() {
        applyRequiredPlugins();
        configureStaticAnalysisTools();
        new CpdTaskRegistration(project, extension).register();
        registerSpotlessOnEnforcedFormatting();
    }

    private void applyRequiredPlugins() {
        final var pluginManager = project.getPluginManager();
        pluginManager.apply("java");
        pluginManager.apply("pmd");
        pluginManager.apply("checkstyle");
        pluginManager.apply("jacoco");
        pluginManager.apply(SpotBugsPlugin.class);
    }

    private void configureStaticAnalysisTools() {
        configurePmd();
        configureCheckstyle();
        configureJacoco();
        configureSpotBugs();
    }

    private void registerSpotlessOnEnforcedFormatting() {
        project.afterEvaluate(p -> {
            if (extension.getEnforceFormatting().get()) {
                p.getPluginManager().apply(SpotlessPlugin.class);
                configureSpotless(p);
            }
        });
    }

    private void configurePmd() {
        project.getExtensions().configure(PmdExtension.class, pmd -> {
            pmd.setConsoleOutput(false);
            pmd.setIgnoreFailures(true);
            pmd.setToolVersion("7.9.0");
        });
        project.getTasks().withType(Pmd.class).configureEach(task ->
                task.getReports().getXml().getRequired().set(true));
    }

    private void configureCheckstyle() {
        project.getExtensions().configure(CheckstyleExtension.class, cs -> {
            cs.setIgnoreFailures(true);
            cs.setToolVersion("10.21.4");
            final var defaultConfigFile = project.file("config/checkstyle/checkstyle.xml");
            if (!defaultConfigFile.exists()) {
                final String configContent = loadClasspathResource("/cleancode-checkstyle.xml");
                final var bundledConfig = project.getResources().getText().fromString(configContent);
                cs.setConfig(bundledConfig);
            }
        });
        project.getTasks().withType(Checkstyle.class).configureEach(task ->
                task.getReports().getXml().getRequired().set(true));
    }

    private void configureJacoco() {
        project.getExtensions().configure(JacocoPluginExtension.class, jacoco ->
                jacoco.setToolVersion("0.8.12"));

        project.getTasks().withType(JacocoReport.class).configureEach(task -> {
            task.getReports().getXml().getRequired().set(true);
            task.dependsOn(project.getTasks().named("test"));
        });
    }

    private void configureSpotless(final Project target) {
        target.getRepositories().mavenCentral();
        target.getExtensions().configure(SpotlessExtension.class, spotless -> {
            spotless.java(java -> {
                java.target("src/**/*.java");
                java.googleJavaFormat().aosp().reflowLongStrings();
                java.removeUnusedImports();
                java.trimTrailingWhitespace();
                java.endWithNewline();
            });
        });
    }

    private void configureSpotBugs() {
        project.getExtensions().configure(SpotBugsExtension.class, sb -> {
            sb.getIgnoreFailures().set(true);
            sb.getToolVersion().set("4.9.3");
        });
        project.getTasks().withType(SpotBugsTask.class).configureEach(task -> {
            final var xmlReport = task.getReports().create("xml");
            xmlReport.getRequired().set(true);
            final var spotBugsXmlReportLocation =
                    project.getLayout().getBuildDirectory().file("reports/spotbugs/main.xml");
            xmlReport.getOutputLocation().set(spotBugsXmlReportLocation);
        });
    }

    private String loadClasspathResource(final String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Classpath resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read classpath resource: " + resourcePath, e);
        }
    }
}
