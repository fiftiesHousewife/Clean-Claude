package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.adapters.CheckstyleFindingSource;
import org.fiftieshousewife.cleancode.adapters.CpdFindingSource;
import org.fiftieshousewife.cleancode.adapters.DependencyUpdatesFindingSource;
import org.fiftieshousewife.cleancode.adapters.JacocoFindingSource;
import org.fiftieshousewife.cleancode.adapters.OpenRewriteFindingSource;
import org.fiftieshousewife.cleancode.adapters.PmdFindingSource;
import org.fiftieshousewife.cleancode.adapters.SpotBugsFindingSource;
import org.fiftieshousewife.cleancode.adapters.SurefireFindingSource;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewFindingSource;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.BaselineManager;
import org.fiftieshousewife.cleancode.core.BuildOutputFormatter;
import org.fiftieshousewife.cleancode.core.FindingAggregator;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.HtmlReportWriter;
import org.fiftieshousewife.cleancode.core.JsonReportWriter;
import org.fiftieshousewife.cleancode.core.PackageSuppression;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AnalyseTask extends DefaultTask {

    private static final String REPORTS_DIR = "reports/clean-code";
    private static final String HTML_REPORT_NAME = "findings.html";
    private static final String JSON_REPORT_NAME = "findings.json";
    private static final String BASELINE_FILE_NAME = "clean-code-baseline.json";

    @TaskAction
    public void analyse() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);

        final ProjectContext context = buildProjectContext(projectRoot, buildDir);
        final List<FindingSource> sources = buildFindingSources(ext);
        final AggregatedReport report = aggregateAndFilter(sources, context, ext);

        final Path htmlReport = writeReports(report, buildDir.resolve(REPORTS_DIR), ext, projectRoot);
        logSummary(report, projectRoot, htmlReport);
    }

    private ProjectContext buildProjectContext(final Path projectRoot, final Path buildDir) {
        return new ProjectContext(
                projectRoot,
                getProject().getName(),
                getProject().getVersion().toString(),
                "21",
                List.of(projectRoot.resolve("src/main/java")),
                List.of(projectRoot.resolve("src/test/java")),
                buildDir,
                buildDir.resolve("reports"),
                RuntimeDependencies.resolve(getProject()));
    }

    private List<FindingSource> buildFindingSources(final CleanCodeExtension ext) {
        final RecipeThresholds thresholds = ext.buildRecipeThresholds();
        final ClaudeReviewConfig claudeConfig = ext.buildClaudeReviewConfig(resolveApiKey());
        return List.of(
                new PmdFindingSource(),
                new CheckstyleFindingSource(),
                new SpotBugsFindingSource(),
                new CpdFindingSource(),
                new JacocoFindingSource(),
                new SurefireFindingSource(),
                new DependencyUpdatesFindingSource(),
                new OpenRewriteFindingSource(thresholds),
                new ClaudeReviewFindingSource(claudeConfig));
    }

    private AggregatedReport aggregateAndFilter(final List<FindingSource> sources,
                                                final ProjectContext context,
                                                final CleanCodeExtension ext) throws Exception {
        final Set<String> disabledRecipes = Set.copyOf(ext.getDisabledRecipes().get());
        final PackageSuppression packageSuppression = PackageSuppression.of(ext.getPackageSuppressions().get());
        final AggregatedReport fullReport = FindingAggregator.aggregate(sources, context);
        final AggregatedReport filteredByCode = ReportFiltering.filterDisabledRecipes(fullReport, disabledRecipes);
        return ReportFiltering.filterSuppressions(filteredByCode, context.projectRoot(), packageSuppression);
    }

    private Path writeReports(final AggregatedReport report,
                              final Path outputDir,
                              final CleanCodeExtension ext,
                              final Path projectRoot) throws Exception {
        final String repositoryUrl = buildRepositoryUrl(ext, projectRoot);
        final Path htmlReport = outputDir.resolve(HTML_REPORT_NAME);
        JsonReportWriter.write(report, outputDir.resolve(JSON_REPORT_NAME));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl);
        return htmlReport;
    }

    private String buildRepositoryUrl(final CleanCodeExtension ext, final Path projectRoot) {
        final String baseRepoUrl = ext.getRepositoryUrl().get();
        final String modulePath = getProject().getRootDir().toPath().relativize(projectRoot).toString();
        return baseRepoUrl.isBlank() ? ""
                : baseRepoUrl + "/blob/main" + (modulePath.isEmpty() ? "" : "/" + modulePath);
    }

    private void logSummary(final AggregatedReport report, final Path projectRoot, final Path htmlReport)
            throws Exception {
        final Path baselineFile = projectRoot.resolve(BASELINE_FILE_NAME);
        final Map<HeuristicCode, BaselineManager.Delta> deltas = Files.exists(baselineFile)
                ? BaselineManager.computeDeltas(report, baselineFile)
                : Map.of();
        getLogger().lifecycle(BuildOutputFormatter.format(report, deltas));
        getLogger().lifecycle("\n  Report: file://" + htmlReport.toAbsolutePath());
    }

    private String resolveApiKey() {
        final Object prop = getProject().findProperty("ANTHROPIC_API_KEY");
        if (prop != null && !prop.toString().isBlank()) {
            return prop.toString();
        }
        final String env = System.getenv("ANTHROPIC_API_KEY");
        return env != null ? env : "";
    }
}
