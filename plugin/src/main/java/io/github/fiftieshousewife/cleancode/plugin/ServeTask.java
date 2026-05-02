package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.HtmlReportWriter;
import io.github.fiftieshousewife.cleancode.core.JsonReportWriter;
import io.github.fiftieshousewife.cleancode.core.SourceState;
import io.github.fiftieshousewife.cleancode.plugin.serve.ApplyChangesResponse;
import io.github.fiftieshousewife.cleancode.plugin.serve.ConfigSnapshot;
import io.github.fiftieshousewife.cleancode.plugin.serve.ReportServer;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Long-running task that runs the analysis once, opens the HTML report
 * in a browser, and serves it from {@code http://localhost:<port>} so
 * the in-page UI can call back into the server with batched changes
 * (disable a recipe, tune a threshold, suppress a finding).
 *
 * <p>This commit wires the server skeleton only — apply-changes is
 * stubbed; the next commits add the staging UI and the actual mutations.
 */
@DisableCachingByDefault(because = "long-running interactive task")
public abstract class ServeTask extends DefaultTask {

    @TaskAction
    public void serve() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path outputDir = buildDir.resolve("reports/clean-code");
        final Path htmlReport = outputDir.resolve("findings.html");

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

        final int port = ext.getServePort().get();
        final ReportServer server = ReportServer.start(
                port,
                () -> htmlReport,
                () -> snapshotConfig(ext),
                request -> {
                    getLogger().lifecycle("apply-changes received: {} change(s)",
                            request.changes() == null ? 0 : request.changes().size());
                    if (request.changes() != null) {
                        request.changes().forEach(c ->
                                getLogger().lifecycle("  staged: {} {} reason='{}'",
                                        c.kind(), c.params(), c.reason()));
                    }
                    return ApplyChangesResponse.ok(
                            request.changes() == null ? 0 : request.changes().size());
                },
                getLogger());

        getLogger().lifecycle("\n  Clean Code report serving at: {}", server.url());
        getLogger().lifecycle("  Press Ctrl-C to stop.\n");

        openInBrowser(server.url());

        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().lifecycle("\n  Stopping serve task...");
            server.stop();
            shutdownLatch.countDown();
        }));
        shutdownLatch.await();
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
