package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.JsonReportReader;
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

/**
 * Single-command entry point for the {@link ReworkOrchestrator}. Takes a
 * {@code -Pfile=<path>} and an optional {@code -Pmode=SUGGEST_ONLY|AGENT_DRIVEN}
 * (defaults to {@code SUGGEST_ONLY}), reads the latest
 * {@code build/reports/clean-code/findings.json}, runs the orchestrator,
 * and writes the commit-message body alongside the findings report.
 *
 * <p>Everything the agent-harness flow needs lives here and in the
 * {@code rework} sub-package — one user-facing task, no proliferation
 * of shell scripts.
 */
public abstract class ReworkClassTask extends DefaultTask {

    private static final String FINDINGS_JSON = "reports/clean-code/findings.json";
    private static final String REWORK_OUTPUT_DIR = "reports/clean-code";

    @TaskAction
    public void rework() throws IOException {
        final String fileProperty = requiredProperty("file");
        final String modeProperty = optionalProperty("mode", ReworkMode.SUGGEST_ONLY.name());
        final String variantProperty = optionalProperty("variant", RunVariant.MCP_RECIPES.name());
        final ReworkMode mode = parseMode(modeProperty);
        final RunVariant variant = parseVariant(variantProperty);

        final Path projectRoot = getProject().getRootDir().toPath();
        final Path target = resolveTarget(projectRoot, fileProperty);
        final AggregatedReport report = loadReport();

        final ReworkReport result = runOrchestrator(target, projectRoot, report, mode, variant);
        final Path messageFile = writeMessageBody(result);
        logSummary(result, messageFile, variant);
    }

    private String requiredProperty(final String name) {
        final Object value = getProject().findProperty(name);
        if (value == null || value.toString().isBlank()) {
            throw new GradleException("missing -P" + name + "=<value>");
        }
        return value.toString();
    }

    private String optionalProperty(final String name, final String fallback) {
        final Object value = getProject().findProperty(name);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private static ReworkMode parseMode(final String raw) {
        try {
            return ReworkMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new GradleException("unknown mode `" + raw + "` — expected SUGGEST_ONLY or AGENT_DRIVEN");
        }
    }

    private static RunVariant parseVariant(final String raw) {
        try {
            return RunVariant.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new GradleException("unknown variant `" + raw
                    + "` — expected VANILLA, MCP_GRADLE_ONLY, or MCP_RECIPES");
        }
    }

    private static Path resolveTarget(final Path projectRoot, final String raw) {
        final Path given = Path.of(raw);
        return given.isAbsolute() ? given : projectRoot.resolve(given);
    }

    private AggregatedReport loadReport() throws IOException {
        final Path findings = getProject().getLayout().getBuildDirectory()
                .file(FINDINGS_JSON).get().getAsFile().toPath();
        if (!Files.exists(findings)) {
            throw new GradleException("no findings.json — run `./gradlew analyseCleanCode` first");
        }
        return JsonReportReader.read(findings);
    }

    private ReworkReport runOrchestrator(final Path target, final Path projectRoot,
                                         final AggregatedReport report, final ReworkMode mode,
                                         final RunVariant variant) {
        try {
            return new ReworkOrchestrator()
                    .reworkClass(target, projectRoot, report, mode, variant);
        } catch (ReworkOrchestrator.ReworkException e) {
            throw new GradleException("rework failed: " + e.getMessage(), e);
        }
    }

    private Path writeMessageBody(final ReworkReport result) throws IOException {
        final Path outputDir = getProject().getLayout().getBuildDirectory()
                .dir(REWORK_OUTPUT_DIR).get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        final String baseName = result.files().getFirst().getFileName().toString().replace(".java", "");
        final Path destination = outputDir.resolve(baseName + "-rework.md");
        Files.writeString(destination, result.commitMessageBody());
        return destination;
    }

    private void logSummary(final ReworkReport result, final Path messageFile, final RunVariant variant) {
        getLogger().lifecycle("Rework mode: {}{}",
                result.mode(),
                result.mode() == ReworkMode.AGENT_DRIVEN ? " (" + variant + ")" : "");
        getLogger().lifecycle("  suggestions: {}", result.suggestions().size());
        getLogger().lifecycle("  actions    : {}", result.actionsTaken().size());
        getLogger().lifecycle("  rejected   : {}", result.rejected().size());
        getLogger().lifecycle("Commit message body written to: {}", messageFile);
    }
}
