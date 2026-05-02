package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.HtmlReportWriter;
import io.github.fiftieshousewife.cleancode.core.JsonReportWriter;
import io.github.fiftieshousewife.cleancode.core.SourceState;
import io.github.fiftieshousewife.cleancode.plugin.serve.ApplyChangesResponse;
import io.github.fiftieshousewife.cleancode.plugin.serve.ChangeApplier;
import io.github.fiftieshousewife.cleancode.plugin.serve.ConfigSnapshot;
import io.github.fiftieshousewife.cleancode.plugin.serve.ReportServer;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Long-running task that runs the analysis once, opens the HTML report
 * in a browser, and serves it from {@code http://localhost:<port>} so
 * the in-page UI can call back into the server with batched changes
 * (disable a recipe, tune a threshold, suppress a finding).
 *
 * <p>Apply handler runs the staged changes against the project root via
 * {@link ChangeApplier}, then re-runs the analysis and rewrites the
 * report. The client soft-reloads on success so fresh state is shown.
 */
@DisableCachingByDefault(because = "long-running interactive task")
public abstract class ServeTask extends DefaultTask {

    @TaskAction
    public void serve() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path outputDir = buildDir.resolve("reports/clean-code");
        final Path htmlReport = outputDir.resolve("findings.html");
        final Path pidFile = buildDir.resolve("clean-code/serve.pid");

        final SandboxAnalysis.Result analysis = SandboxAnalysis.analyseWithStates(getProject());
        final AggregatedReport report = analysis.report();
        final List<SourceState> sourceStates = analysis.sourceStates();

        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final String baseRepoUrl = ext.getRepositoryUrl().get();
        final String modulePath = getProject().getRootDir().toPath().relativize(projectRoot).toString();
        final String repositoryUrl = baseRepoUrl.isBlank() ? ""
                : baseRepoUrl + "/blob/main" + (modulePath.isEmpty() ? "" : "/" + modulePath);
        final String ideScheme = ext.getIdeUrlScheme().getOrElse("").isBlank()
                ? AnalyseTask.detectIdeUrlScheme(projectRoot)
                : ext.getIdeUrlScheme().get();

        JsonReportWriter.write(report, outputDir.resolve("findings.json"));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl, projectRoot, ideScheme, sourceStates);

        writePidFile(pidFile);

        final int port = ext.getServePort().get();
        final ChangeApplier applier = new ChangeApplier(projectRoot);

        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final AtomicBoolean shuttingDown = new AtomicBoolean(false);
        final AtomicReference<ReportServer> serverRef = new AtomicReference<>();
        final Runnable shutdown = () -> {
            if (!shuttingDown.compareAndSet(false, true)) {
                return;
            }
            final ReportServer running = serverRef.get();
            if (running != null) {
                running.stop();
            }
            deletePidFile(pidFile);
            shutdownLatch.countDown();
        };

        final ReportServer server = ReportServer.start(
                port,
                () -> htmlReport,
                () -> snapshotConfig(ext),
                request -> applyAndRegenerate(applier, request.changes(),
                        outputDir, htmlReport, repositoryUrl, ideScheme),
                shutdown,
                getLogger());
        serverRef.set(server);

        getLogger().lifecycle("\n  Clean Code report serving at: {}", server.url());
        getLogger().lifecycle("  Press Ctrl-C or run ./gradlew cleanCodeStop to shut down.\n");

        openInBrowser(server.url());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().lifecycle("\n  Stopping serve task...");
            shutdown.run();
        }, "cleanCodeServe-shutdown"));
        shutdownLatch.await();
    }

    private void writePidFile(final Path pidFile) {
        try {
            Files.createDirectories(pidFile.getParent());
            Files.writeString(pidFile, Long.toString(ProcessHandle.current().pid()));
        } catch (IOException e) {
            getLogger().warn("Could not write PID file at {}: {}", pidFile, e.getMessage());
        }
    }

    private void deletePidFile(final Path pidFile) {
        try {
            Files.deleteIfExists(pidFile);
        } catch (IOException e) {
            getLogger().warn("Could not delete PID file at {}: {}", pidFile, e.getMessage());
        }
    }

    private ApplyChangesResponse applyAndRegenerate(final ChangeApplier applier,
                                                     final List<io.github.fiftieshousewife.cleancode.plugin.serve.PendingChange> changes,
                                                     final Path outputDir, final Path htmlReport,
                                                     final String repositoryUrl, final String ideScheme) {
        if (changes == null || changes.isEmpty()) {
            return ApplyChangesResponse.ok(0);
        }
        getLogger().lifecycle("applying {} staged change(s)...", changes.size());
        final ApplyChangesResponse applyResponse = applier.apply(changes);
        if (!applyResponse.success()) {
            applyResponse.errors().forEach(err -> getLogger().error("  apply error: {}", err));
            return applyResponse;
        }

        try {
            getLogger().lifecycle("re-running analysis...");
            final SandboxAnalysis.Result rerun = SandboxAnalysis.analyseWithStates(getProject());
            JsonReportWriter.write(rerun.report(), outputDir.resolve("findings.json"));
            HtmlReportWriter.write(rerun.report(), htmlReport, repositoryUrl,
                    getProject().getProjectDir().toPath(), ideScheme, rerun.sourceStates());
            getLogger().lifecycle("  applied {} change(s); {} findings remain.",
                    applyResponse.applied(), rerun.report().findings().size());
        } catch (Exception e) {
            getLogger().error("re-run after apply failed", e);
            return ApplyChangesResponse.failed(java.util.List.of(
                    "changes were applied to disk but re-running analysis failed: " + e.getMessage()
                            + " (re-run ./gradlew cleanCodeServe to refresh)"));
        }
        return applyResponse;
    }

    private void openInBrowser(final String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            getLogger().info("Could not auto-open browser ({}); navigate manually to {}",
                    e.getMessage(), url);
        }
    }

    private ConfigSnapshot snapshotConfig(final CleanCodeExtension ext) {
        final Map<String, Integer> thresholds = Map.of(
                "classLineCount", ext.getThresholds().getClassLineCount().get(),
                "recordComponentCount", ext.getThresholds().getRecordComponentCount().get(),
                "methodBlankLineSections", ext.getThresholds().getMethodBlankLineSections().get(),
                "privateMethodMinLines", ext.getThresholds().getPrivateMethodMinLines().get(),
                "magicStringMinOccurrences", ext.getThresholds().getMagicStringMinOccurrences().get(),
                "magicNumberMinValue", ext.getThresholds().getMagicNumberMinValue().get(),
                "hardcodedListMinLiterals", ext.getThresholds().getHardcodedListMinLiterals().get(),
                "cpdMinimumTokens", ext.getThresholds().getCpdMinimumTokens().get());
        return new ConfigSnapshot(
                List.copyOf(ext.getDisabledRecipes().get()),
                List.copyOf(ext.getEnabledOptionalRules().get()),
                thresholds);
    }
}
