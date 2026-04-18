package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.fiftieshousewife.cleancode.plugin.rework.ComparisonReport;
import org.fiftieshousewife.cleancode.plugin.rework.DefaultAgentRunner;
import org.fiftieshousewife.cleancode.plugin.rework.GitWorkingTree;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkMode;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkOrchestrator;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkReport;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Runs the rework flow twice against a sandbox fixture — once with the
 * refactoring recipes advertised to the agent, once without — and
 * writes a side-by-side comparison of the commit-message bodies, the
 * diffs, and the token accounting. One command, no manual interleave
 * of runs and git restores.
 *
 * <p>Refuses to run unless the target lives under {@code sandbox/} —
 * the task mutates the file in place between runs, so pointing it at
 * real source risks losing work.
 */
public abstract class ReworkCompareTask extends DefaultTask {

    private static final String SANDBOX_PREFIX = "sandbox/";
    private static final String FINDINGS_JSON = "reports/clean-code/findings.json";
    private static final String OUTPUT_DIR = "reports/clean-code";

    @TaskAction
    public void compare() throws IOException {
        final String fileProperty = requiredProperty("file");
        ensureSandboxPath(fileProperty);

        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path target = projectRoot.resolve(fileProperty);
        final GitWorkingTree git = new GitWorkingTree(projectRoot);
        ensureWorkingTreeClean(git, target);

        final AggregatedReport findings = loadFindings();
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(
                new DefaultAgentRunner(line -> getLogger().lifecycle("    {}", line)),
                Duration.ofMinutes(15));

        getLogger().lifecycle("▶ run 1 of 2 — with recipe tools");
        final ReworkReport withTools = runAndCaptureDiff(orchestrator, target, projectRoot, findings, true, git);
        final String diffWithTools = captureDiff(git, target);
        restore(git, target);

        getLogger().lifecycle("▶ run 2 of 2 — without recipe tools");
        final ReworkReport withoutTools = runAndCaptureDiff(orchestrator, target, projectRoot, findings, false, git);
        final String diffWithoutTools = captureDiff(git, target);
        restore(git, target);

        final Path destination = writeComparison(target,
                ComparisonReport.format(withTools, diffWithTools, withoutTools, diffWithoutTools));
        logSummary(withTools, withoutTools, destination);
    }

    private String requiredProperty(final String name) {
        final Object value = getProject().findProperty(name);
        if (value == null || value.toString().isBlank()) {
            throw new GradleException("missing -P" + name + "=<value>");
        }
        return value.toString();
    }

    private static void ensureSandboxPath(final String path) {
        if (!path.startsWith(SANDBOX_PREFIX)) {
            throw new GradleException("reworkCompare targets must live under `sandbox/` — got: " + path);
        }
    }

    private static void ensureWorkingTreeClean(final GitWorkingTree git, final Path file) {
        try {
            if (!git.isClean(file)) {
                throw new GradleException("working tree for " + file
                        + " is dirty — commit or stash first so the paired runs have a clean baseline");
            }
        } catch (GitWorkingTree.GitException e) {
            throw new GradleException("git precheck failed: " + e.getMessage(), e);
        }
    }

    private AggregatedReport loadFindings() throws IOException {
        final Path findings = getProject().getLayout().getBuildDirectory()
                .file(FINDINGS_JSON).get().getAsFile().toPath();
        if (!Files.exists(findings)) {
            throw new GradleException("no findings.json — run `./gradlew analyseCleanCode` first");
        }
        return JsonReportReader.read(findings);
    }

    private ReworkReport runAndCaptureDiff(final ReworkOrchestrator orchestrator,
                                           final Path target, final Path projectRoot,
                                           final AggregatedReport findings,
                                           final boolean includeRecipes,
                                           final GitWorkingTree git) {
        try {
            return orchestrator.reworkClass(target, projectRoot, findings,
                    ReworkMode.AGENT_DRIVEN, includeRecipes);
        } catch (ReworkOrchestrator.ReworkException e) {
            throw new GradleException("rework run failed (includeRecipes=" + includeRecipes + "): "
                    + e.getMessage(), e);
        }
    }

    private static String captureDiff(final GitWorkingTree git, final Path file) {
        try {
            return git.diff(file);
        } catch (GitWorkingTree.GitException e) {
            throw new GradleException("git diff failed: " + e.getMessage(), e);
        }
    }

    private static void restore(final GitWorkingTree git, final Path file) {
        try {
            git.restore(file);
        } catch (GitWorkingTree.GitException e) {
            throw new GradleException("git restore failed: " + e.getMessage(), e);
        }
    }

    private Path writeComparison(final Path target, final String markdown) throws IOException {
        final Path outputDir = getProject().getLayout().getBuildDirectory()
                .dir(OUTPUT_DIR).get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        final String baseName = target.getFileName().toString().replace(".java", "");
        final Path destination = outputDir.resolve(baseName + "-comparison.md");
        Files.writeString(destination, markdown);
        return destination;
    }

    private void logSummary(final ReworkReport withTools, final ReworkReport withoutTools,
                            final Path destination) {
        getLogger().lifecycle("Comparison written to: {}", destination);
        getLogger().lifecycle("  with recipe tools    — actions: {}  rejected: {}",
                withTools.actionsTaken().size(), withTools.rejected().size());
        getLogger().lifecycle("  without recipe tools — actions: {}  rejected: {}",
                withoutTools.actionsTaken().size(), withoutTools.rejected().size());
    }
}
