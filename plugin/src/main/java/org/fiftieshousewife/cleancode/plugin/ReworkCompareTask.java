package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
import org.fiftieshousewife.cleancode.plugin.rework.ComparisonReport;
import org.fiftieshousewife.cleancode.plugin.rework.DefaultAgentRunner;
import org.fiftieshousewife.cleancode.plugin.rework.FindingsSnapshot;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final List<RunVariant> ALL_VARIANTS = List.of(
            RunVariant.VANILLA, RunVariant.MCP_GRADLE_ONLY,
            RunVariant.MCP_RECIPES, RunVariant.HARNESS_RECIPES_THEN_AGENT);
    private static final int DEFAULT_MAX_RETRIES = 1;

    @TaskAction
    public void compare() throws IOException {
        final List<String> relativePaths = collectFileProperty();
        relativePaths.forEach(ReworkCompareTask::ensureSandboxPath);
        final Path projectRoot = getProject().getRootDir().toPath();
        final List<Path> targets = relativePaths.stream()
                .map(projectRoot::resolve).toList();
        final Set<Path> targetSet = new HashSet<>(targets);
        final Path sandboxDir = getProject().getProjectDir().toPath();
        final GitWorkingTree git = new GitWorkingTree(projectRoot);
        ensureWorkingTreeClean(git, targets);
        final AggregatedReport baselineFindings = loadFindings();
        final List<RunVariant> variants = collectVariantsProperty();
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(
                new DefaultAgentRunner(line -> getLogger().lifecycle("    {}", line)),
                Duration.ofMinutes(20));
        final List<ComparisonReport.VariantRun> runs = runAllVariants(
                orchestrator, variants, targets, targetSet, sandboxDir,
                projectRoot, baselineFindings, git);
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

    private List<RunVariant> collectVariantsProperty() {
        final Object raw = getProject().findProperty("variants");
        if (raw == null || raw.toString().isBlank()) {
            return ALL_VARIANTS;
        }
        final List<RunVariant> selected = new ArrayList<>();
        for (final String part : raw.toString().split(",")) {
            final String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                selected.add(RunVariant.valueOf(trimmed));
            } catch (IllegalArgumentException e) {
                throw new GradleException("unknown variant `" + trimmed + "` — expected one of "
                        + ALL_VARIANTS + " (comma-separated, case-sensitive)");
            }
        }
        if (selected.isEmpty()) {
            throw new GradleException("-Pvariants must name at least one variant");
        }
        return selected;
    }

    private List<ComparisonReport.VariantRun> runAllVariants(final ReworkOrchestrator orchestrator,
                                                             final List<RunVariant> variants,
                                                             final List<Path> targets,
                                                             final Set<Path> targetSet,
                                                             final Path sandboxDir,
                                                             final Path projectRoot,
                                                             final AggregatedReport baselineFindings,
                                                             final GitWorkingTree git) {
        final int maxRetries = maxRetriesProperty();
        final ReworkOrchestrator.Options options = ReworkOrchestrator.Options.withFeedbackLoop(
                this::analyseFresh, maxRetries, sandboxDir);
        final List<ComparisonReport.VariantRun> results = new ArrayList<>();
        int index = 0;
        for (final RunVariant variant : variants) {
            index++;
            getLogger().lifecycle("▶ run {} of {} — {}", index, variants.size(), variant);
            final ReworkReport report = invokeOrchestrator(
                    orchestrator, targets, projectRoot, baselineFindings, variant, options);
            final FindingsSnapshot snapshot = computeSnapshot(baselineFindings, targetSet, sandboxDir, variant);
            results.add(new ComparisonReport.VariantRun(variant, report, captureDiff(git, targets), snapshot));
            restore(git, targets);
        }
        return results;
    }

    private int maxRetriesProperty() {
        final Object raw = getProject().findProperty("feedbackRetries");
        if (raw == null || raw.toString().isBlank()) {
            return DEFAULT_MAX_RETRIES;
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new GradleException("-PfeedbackRetries must be a non-negative integer; got: " + raw, e);
        }
    }

    private AggregatedReport analyseFresh() {
        try {
            return SandboxAnalysis.analyse(getProject());
        } catch (FindingSourceException e) {
            throw new GradleException("re-analysis failed: " + e.getMessage(), e);
        }
    }

    private FindingsSnapshot computeSnapshot(final AggregatedReport baseline,
                                             final Set<Path> targetSet,
                                             final Path sandboxDir,
                                             final RunVariant variant) {
        try {
            final AggregatedReport current = SandboxAnalysis.analyse(getProject());
            return FindingsSnapshot.compute(
                    baseline.findings(), current.findings(), targetSet, sandboxDir);
        } catch (FindingSourceException | RuntimeException e) {
            throw new GradleException(
                    "re-analysis failed after variant " + variant + ": " + e.getMessage(), e);
        }
    }

    private static ReworkReport invokeOrchestrator(final ReworkOrchestrator orchestrator,
                                                   final List<Path> targets, final Path projectRoot,
                                                   final AggregatedReport findings,
                                                   final RunVariant variant,
                                                   final ReworkOrchestrator.Options options) {
        try {
            return orchestrator.reworkClasses(targets, projectRoot, findings,
                    ReworkMode.AGENT_DRIVEN, variant, options);
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
        runs.forEach(run -> getLogger().lifecycle(summaryLine(run)));
    }

    private static String summaryLine(final ComparisonReport.VariantRun run) {
        final ReworkReport report = run.report();
        final FindingsSnapshot findings = run.findings();
        final StringBuilder line = new StringBuilder(120);
        line.append("  ").append(run.variant())
                .append(" — actions: ").append(report.actionsTaken().size())
                .append("  rejected: ").append(report.rejected().size())
                .append("  findings: ").append(findings.baseline()).append("→").append(findings.finalCount())
                .append(" (fixed ").append(findings.fixed())
                .append(", introduced ").append(findings.introduced()).append(')');
        report.usage().ifPresent(usage -> line
                .append("  wall: ").append(String.format("%.1fs", usage.durationMs() / 1000.0))
                .append("  cost: ").append(String.format("$%.4f", usage.totalCostUsd()))
                .append("  tokens: ").append(usage.totalInputTokens()).append(" in / ")
                .append(usage.outputTokens()).append(" out")
                .append("  turns: ").append(usage.numTurns()));
        return line.toString();
    }
}
