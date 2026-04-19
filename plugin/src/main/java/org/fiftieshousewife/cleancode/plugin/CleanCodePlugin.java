package org.fiftieshousewife.cleancode.plugin;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.github.spotbugs.snom.SpotBugsExtension;
import com.github.spotbugs.snom.SpotBugsPlugin;
import com.github.spotbugs.snom.SpotBugsTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        applyStaticAnalysisPlugins(project, ext);

        project.afterEvaluate(p -> {
            final Path skillsDir = p.getProjectDir().toPath().resolve(ext.getSkillsDir().get());
            new SkillFileScaffolder(skillsDir, ext.getThresholds(), p.getLogger()).scaffold();
        });

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
                .register("cleanCodeFixPlan", FixPlanTask.class, task -> {
                    task.setDescription("Group findings by file into per-class fix briefs for agent handoff");
                    task.setGroup("verification");
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("cleanCodeExplain", ExplainTask.class, task -> {
                    task.setDescription("Print skill guidance for a finding concern");
                    task.setGroup("help");
                });

        project.getTasks()
                .register("reworkClass", ReworkClassTask.class, task -> {
                    task.setDescription(
                            "Rework a single class via the ReworkOrchestrator Java API "
                                    + "(default mode SUGGEST_ONLY; pass -Pmode=AGENT_DRIVEN for claude -p)");
                    task.setGroup("clean code");
                    task.dependsOn(analyse);
                });

        project.getTasks()
                .register("reworkCompare", ReworkCompareTask.class, task -> {
                    task.setDescription(
                            "Run rework twice (with and without recipe tools) on a sandbox fixture "
                                    + "and produce a side-by-side comparison with diffs and token cost");
                    task.setGroup("clean code");
                    task.dependsOn(analyse);
                });

        if (project.getRootProject().equals(project)) {
            project.getTasks()
                    .register("updateVersionCatalog", UpdateVersionCatalogTask.class, task -> {
                        task.setDescription(
                                "Rewrite gradle/libs.versions.toml with the non-major "
                                        + "updates from the dependencyUpdates report");
                        task.setGroup("verification");
                    });

            project.getTasks()
                    .register("cleanCodeSummary", SummaryReportTask.class, task -> {
                        task.setDescription(
                                "Aggregate every module's findings.json into "
                                        + "docs/reports/index.html with totals and links");
                        task.setGroup("verification");
                    });
        }
    }

    private void applyStaticAnalysisPlugins(Project project, CleanCodeExtension ext) {
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("pmd");
        project.getPluginManager().apply("checkstyle");
        project.getPluginManager().apply("jacoco");
        project.getPluginManager().apply(SpotBugsPlugin.class);

        configurePmd(project);
        configureCheckstyle(project);
        configureJacoco(project);
        configureSpotBugs(project);
        registerCpdTask(project, ext);

        project.afterEvaluate(p -> {
            if (ext.getEnforceFormatting().get()) {
                p.getPluginManager().apply(SpotlessPlugin.class);
                configureSpotless(p);
            }
        });
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

    private void configureSpotless(Project project) {
        project.getRepositories().mavenCentral();
        project.getExtensions().configure(SpotlessExtension.class, spotless -> {
            spotless.java(java -> {
                java.target("src/**/*.java");
                java.googleJavaFormat().aosp().reflowLongStrings();
                java.removeUnusedImports();
                java.trimTrailingWhitespace();
                java.endWithNewline();
            });
        });
    }

    private void configureSpotBugs(Project project) {
        project.getExtensions().configure(SpotBugsExtension.class, sb -> {
            sb.getIgnoreFailures().set(true);
            // SpotBugs 4.9.7 added support for class file major version 69
            // (JDK 25). Earlier versions throw IllegalArgumentException
            // when scanning JDK 25 bytecode.
            sb.getToolVersion().set("4.9.8");
        });
        project.getTasks().withType(SpotBugsTask.class).configureEach(task -> {
            final var xmlReport = task.getReports().create("xml");
            xmlReport.getRequired().set(true);
            xmlReport.getOutputLocation().set(
                    project.getLayout().getBuildDirectory().file("reports/spotbugs/main.xml"));
        });
    }

    private void registerCpdTask(Project project, CleanCodeExtension ext) {
        final var cpdConfig = project.getConfigurations().create("cpd", conf -> {
            conf.setDescription("CPD classpath");
            conf.setCanBeConsumed(false);
        });

        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-cli:7.9.0");
        project.getDependencies().add("cpd", "net.sourceforge.pmd:pmd-java:7.9.0");

        final var minimumTokens = ext.getThresholds().getCpdMinimumTokens();

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
                    "--minimum-tokens", String.valueOf(minimumTokens.get()),
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
