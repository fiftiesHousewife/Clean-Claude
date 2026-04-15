package org.fiftieshousewife.cleancode.plugin;

import com.github.spotbugs.snom.SpotBugsExtension;
import com.github.spotbugs.snom.SpotBugsTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.Pmd;
import org.gradle.api.plugins.quality.PmdExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public class CleanCodePlugin implements Plugin<Project> {

    private static final String DEPENDENCY_UPDATES_TASK = "dependencyUpdates";

    @Override
    public void apply(Project project) {
        final CleanCodeExtension ext = project.getExtensions()
                .create("cleanCode", CleanCodeExtension.class);

        applyStaticAnalysisPlugins(project);
        scaffoldSkillFiles(project, ext);

        final TaskProvider<AnalyseTask> analyse = project.getTasks()
                .register("analyseCleanCode", AnalyseTask.class, task -> {
                    task.setDescription("Analyse codebase for Clean Code violations");
                    task.setGroup("verification");
                });

        wireTaskDependencies(project, analyse);

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

    private static final List<String> SKILL_FILES = List.of(
            "SKILLS.md",
            "exception-handling.md",
            "null-handling.md",
            "functions.md",
            "classes.md",
            "naming.md",
            "conditionals-and-expressions.md",
            "comments-and-clutter.md",
            "java-idioms.md",
            "project-conventions.md");

    private void scaffoldSkillFiles(Project project, CleanCodeExtension ext) {
        final Path skillsDir = project.getProjectDir().toPath().resolve(ext.getSkillsDir().get());
        try {
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            project.getLogger().warn("Could not create skills directory: {}", skillsDir, e);
            return;
        }

        SKILL_FILES.forEach(filename -> {
            final Path target = skillsDir.resolve(filename);
            if (Files.exists(target)) {
                return;
            }
            try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
                if (is != null) {
                    Files.copy(is, target);
                    project.getLogger().lifecycle("Scaffolded skill file: {}", target);
                }
            } catch (IOException e) {
                project.getLogger().warn("Could not scaffold skill file: {}", filename, e);
            }
        });
    }

    private void applyStaticAnalysisPlugins(Project project) {
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("pmd");
        project.getPluginManager().apply("checkstyle");
        project.getPluginManager().apply("jacoco");
        project.getPluginManager().apply("com.github.spotbugs");

        configurePmd(project);
        configureCheckstyle(project);
        configureJacoco(project);
        configureSpotBugs(project);
        registerCpdTask(project);
    }

    private void configurePmd(Project project) {
        project.getExtensions().configure(PmdExtension.class, pmd -> {
            pmd.setConsoleOutput(false);
            pmd.setIgnoreFailures(true);
            pmd.setToolVersion("7.9.0");
        });
        project.getTasks().withType(Pmd.class).configureEach(task ->
                task.getReports().getXml().getRequired().set(true));
    }

    private void configureCheckstyle(Project project) {
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

    private void configureJacoco(Project project) {
        project.getExtensions().configure(JacocoPluginExtension.class, jacoco ->
                jacoco.setToolVersion("0.8.12"));

        project.getTasks().withType(JacocoReport.class).configureEach(task -> {
            task.getReports().getXml().getRequired().set(true);
            task.dependsOn(project.getTasks().named("test"));
        });
    }

    private void configureSpotBugs(Project project) {
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

    private void registerCpdTask(Project project) {
        final var cpdConfig = project.getConfigurations().create("cpd", conf -> {
            conf.setDescription("CPD classpath");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
        });

        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-cli:7.9.0");
        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-java:7.9.0");

        project.getTasks().register("cpdMain", org.gradle.api.tasks.JavaExec.class, task -> {
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
                    "--minimum-tokens", "50",
                    "--language", "java",
                    "--format", "xml",
                    "--dir", project.file("src/main/java").getAbsolutePath());
        });
    }

    private void wireTaskDependencies(Project project, TaskProvider<AnalyseTask> analyse) {
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

    private String loadClasspathResource(String resourcePath) {
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
