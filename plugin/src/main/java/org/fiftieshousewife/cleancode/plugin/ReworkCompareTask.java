package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.fiftieshousewife.cleancode.plugin.rework.ComparisonReport;
import org.fiftieshousewife.cleancode.plugin.rework.DefaultAgentRunner;
import org.fiftieshousewife.cleancode.plugin.rework.GitWorkingTree;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkMode;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkOrchestrator;
import org.fiftieshousewife.cleancode.plugin.rework.ReworkReport;
import org.fiftieshousewife.cleancode.plugin.rework.RunVariant;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the rework flow once per {@link RunVariant} across one or many
 * sandbox fixtures. The agent gets all of the batched files in a single
 * session, reads each lazily, and emits one JSON summary covering every
 * action. Between variants the target files are restored so each run
 * starts from the same baseline.
 *
 * <p>Accepts {@code -Pfile=<path>} for a single-file target or
 * {@code -Pfiles=<csv>} for a batch. Refuses to run unless every target
 * lives under {@code sandbox/} — the task mutates files in place.
 */
public abstract class ReworkCompareTask extends DefaultTask {

    private static final String SANDBOX_PREFIX = "sandbox/";
    private static final String FINDINGS_JSON = "reports/clean-code/findings.json";
    private static final String OUTPUT_DIR = "reports/clean-code";
    private static final List<RunVariant> VARIANTS = List.of(
            RunVariant.VANILLA, RunVariant.MCP_GRADLE_ONLY, RunVariant.MCP_RECIPES);

    @TaskAction
    public void compare() throws IOException {
        final List<String> relativePaths = collectFileProperty();
        relativePaths.forEach(ReworkCompareTask::ensureSandboxPath);
        final Path projectRoot = getProject().getRootDir().toPath();
        final List<Path> targets = relativePaths.stream()
                .map(projectRoot::resolve).toList();
        final GitWorkingTree git = new GitWorkingTree(projectRoot);
        ensureWorkingTreeClean(git, targets);
        final AggregatedReport findings = loadFindings();
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(
                new DefaultAgentRunner(line -> getLogger().lifecycle("    {}", line)),
                Duration.ofMinutes(20));
        final List<ComparisonReport.VariantRun> runs =
                runAllVariants(orchestrator, targets, projectRoot, findings, git);
        final Path destination = writeComparison(targets, ComparisonReport.format(runs));
        logSummary(runs, destination);
    }

    private List<String> collectFileProperty() {
        final Object files = getProject().findProperty("files");
        if (files != null && !files.toString().isBlank()) {
            final List<String> result = new ArrayList<>();
            for (final String part : files.toString().split(",")) {
                final String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
        final Object file = getProject().findProperty("file");
        if (file == null || file.toString().isBlank()) {
            throw new GradleException("missing -Pfile=<path> or -Pfiles=<comma-separated>");
        }
        return List.of(file.toString());
    }

    private static void ensureSandboxPath(final String path) {
        if (!path.startsWith(SANDBOX_PREFIX)) {
            throw new GradleException("reworkCompare targets must live under `sandbox/` — got: " + path);
        }
    }

    private static void ensureWorkingTreeClean(final GitWorkingTree git, final List<Path> files) {
        try {
            if (!git.isClean(files)) {
                throw new GradleException("working tree for the targets is dirty — "
                        + "commit or stash first so the paired runs have a clean baseline");
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

    private List<ComparisonReport.VariantRun> runAllVariants(final ReworkOrchestrator orchestrator,
                                                             final List<Path> targets,
                                                             final Path projectRoot,
                                                             final AggregatedReport findings,
                                                             final GitWorkingTree git) {
        final List<ComparisonReport.VariantRun> results = new ArrayList<>();
        int index = 0;
        for (final RunVariant variant : VARIANTS) {
            index++;
            getLogger().lifecycle("▶ run {} of {} — {}", index, VARIANTS.size(), variant);
            final ReworkReport report = invokeOrchestrator(orchestrator, targets, projectRoot, findings, variant);
            results.add(new ComparisonReport.VariantRun(variant, report, captureDiff(git, targets)));
            restore(git, targets);
        }
        return results;
    }

    private static ReworkReport invokeOrchestrator(final ReworkOrchestrator orchestrator,
                                                   final List<Path> targets, final Path projectRoot,
                                                   final AggregatedReport findings,
                                                   final RunVariant variant) {
        try {
            return orchestrator.reworkClasses(targets, projectRoot, findings,
                    ReworkMode.AGENT_DRIVEN, variant);
        } catch (ReworkOrchestrator.ReworkException e) {
            throw new GradleException("rework run failed (" + variant + "): " + e.getMessage(), e);
        }
    }

    private static String captureDiff(final GitWorkingTree git, final List<Path> files) {
        try {
            return git.diff(files);
        } catch (GitWorkingTree.GitException e) {
            throw new GradleException("git diff failed: " + e.getMessage(), e);
        }
    }

    private static void restore(final GitWorkingTree git, final List<Path> files) {
        try {
            git.restore(files);
        } catch (GitWorkingTree.GitException e) {
            throw new GradleException("git restore failed: " + e.getMessage(), e);
        }
    }

    private Path writeComparison(final List<Path> targets, final String markdown) throws IOException {
        final Path outputDir = getProject().getLayout().getBuildDirectory()
                .dir(OUTPUT_DIR).get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        final String baseName = targets.size() == 1
                ? targets.getFirst().getFileName().toString().replace(".java", "")
                : "batch-" + targets.size();
        final Path destination = outputDir.resolve(baseName + "-comparison.md");
        Files.writeString(destination, markdown);
        return destination;
    }

    private void logSummary(final List<ComparisonReport.VariantRun> runs, final Path destination) {
        getLogger().lifecycle("Comparison written to: {}", destination);
        runs.forEach(run -> getLogger().lifecycle(
                "  {} — actions: {}  rejected: {}",
                run.variant(), run.report().actionsTaken().size(), run.report().rejected().size()));
    }
}
