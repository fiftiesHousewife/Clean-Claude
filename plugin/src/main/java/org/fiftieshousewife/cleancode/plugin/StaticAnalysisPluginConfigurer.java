package org.fiftieshousewife.cleancode.plugin;

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

final class StaticAnalysisPluginConfigurer {

    private static final String SPOTBUGS_PLUGIN_CLASS = "com.github.spotbugs.snom.SpotBugsPlugin";

    private final Project project;
    private final CleanCodeExtension extension;

    StaticAnalysisPluginConfigurer(final Project project, final CleanCodeExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    void apply() {
        verifySpotBugsOnClasspath();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("pmd");
        project.getPluginManager().apply("checkstyle");
        project.getPluginManager().apply("jacoco");
        project.getPluginManager().apply(SpotBugsPlugin.class);

        configurePmd();
        configureCheckstyle();
        configureJacoco();
        configureSpotBugs();
        new CpdTaskRegistrar(project, extension.getThresholds().getCpdMinimumTokens()).register();
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
                cs.setConfig(project.getResources().getText().fromString(configContent));
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

    private void configureSpotBugs() {
        project.getExtensions().configure(SpotBugsExtension.class, sb -> {
            sb.getIgnoreFailures().set(true);
            sb.getToolVersion().set("4.9.3");
        });
        project.getTasks().withType(SpotBugsTask.class).configureEach(task -> {
            final var xmlReport = task.getReports().create("xml");
            xmlReport.getRequired().set(true);
            xmlReport.getOutputLocation().set(
                    project.getLayout().getBuildDirectory().file("reports/spotbugs/main.xml"));
        });
    }

    private static void verifySpotBugsOnClasspath() {
        try {
            Class.forName(SPOTBUGS_PLUGIN_CLASS,
                    false, StaticAnalysisPluginConfigurer.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new org.gradle.api.GradleException(
                    "The Clean Code plugin requires the SpotBugs Gradle plugin on the plugin classpath, "
                            + "but it was not found. The SpotBugs plugin is published only to the Gradle "
                            + "Plugin Portal — ensure your settings.gradle(.kts) includes it in pluginManagement:\n\n"
                            + "  pluginManagement {\n"
                            + "      repositories {\n"
                            + "          mavenLocal()\n"
                            + "          gradlePluginPortal()\n"
                            + "          mavenCentral()\n"
                            + "      }\n"
                            + "  }\n\n"
                            + "Or apply the bundled init script (see README: 'Apply to another project').");
        }
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
