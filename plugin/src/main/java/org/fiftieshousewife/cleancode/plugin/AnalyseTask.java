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
// TODO: J1 — requires human review: core package exposes more than 8 types used here;
// the wildcard signals this task depends on too many core concerns.
import org.fiftieshousewife.cleancode.core.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AnalyseTask extends DefaultTask {

    private static final String REPORTS_SUBDIR = "reports";
    private static final String CLEAN_CODE_REPORTS_SUBDIR = "reports/clean-code";
    private static final String FINDINGS_JSON = "findings.json";
    private static final String FINDINGS_HTML = "findings.html";
    private static final String MAIN_SOURCES = "src/main/java";
    private static final String TEST_SOURCES = "src/test/java";
    private static final String BASELINE_FILE = "clean-code-baseline.json";
    private static final String JAVA_VERSION = "21";

    @TaskAction
    public void analyse() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();

        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final ProjectContext context = buildProjectContext(projectRoot, buildDir);
        final List<FindingSource> sources = buildFindingSources(ext);

        final AggregatedReport rawReport = FindingAggregator.aggregate(sources, context);
        final AggregatedReport report = applyFilters(rawReport, ext, projectRoot);

        final Path outputDir = buildDir.resolve(CLEAN_CODE_REPORTS_SUBDIR);
        final Path htmlReport = outputDir.resolve(FINDINGS_HTML);
        writeReports(report, outputDir, htmlReport, ext, projectRoot);

        logReport(report, projectRoot.resolve(BASELINE_FILE), htmlReport);
    }

    private ProjectContext buildProjectContext(final Path projectRoot, final Path buildDir) {
        return new ProjectContext(
                projectRoot,
                getProject().getName(),
                getProject().getVersion().toString(),
                JAVA_VERSION,
                List.of(projectRoot.resolve(MAIN_SOURCES)),
                List.of(projectRoot.resolve(TEST_SOURCES)),
                buildDir,
                buildDir.resolve(REPORTS_SUBDIR),
                RuntimeDependencies.groupAndName(getProject()));
    }

    private List<FindingSource> buildFindingSources(final CleanCodeExtension ext) {
        final RecipeThresholds thresholds = ext.buildRecipeThresholds();
        final ClaudeReviewConfig claudeConfig = ext.buildClaudeReviewConfig(resolveApiKey());
        return List.of(
                new PmdFindingSource(),
                new CheckstyleFindingSource(thresholds),
                new SpotBugsFindingSource(),
                new CpdFindingSource(),
                new JacocoFindingSource(),
                new SurefireFindingSource(),
                new DependencyUpdatesFindingSource(),
                new OpenRewriteFindingSource(thresholds),
                new ClaudeReviewFindingSource(claudeConfig));
    }

    private AggregatedReport applyFilters(final AggregatedReport report,
                                          final CleanCodeExtension ext,
                                          final Path projectRoot) {
        final Set<String> disabledRecipes = Set.copyOf(ext.getDisabledRecipes().get());
        final PackageSuppression packageSuppression = PackageSuppression.of(ext.getPackageSuppressions().get());
        final AggregatedReport filteredByCode = filterDisabledRecipes(report, disabledRecipes);
        return filterSuppressions(filteredByCode, projectRoot, packageSuppression);
    }

    private void writeReports(final AggregatedReport report,
                              final Path outputDir,
                              final Path htmlReport,
                              final CleanCodeExtension ext,
                              final Path projectRoot) throws Exception {
        final String repositoryUrl = buildRepositoryUrl(ext, projectRoot);
        JsonReportWriter.write(report, outputDir.resolve(FINDINGS_JSON));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl);
    }

    private String buildRepositoryUrl(final CleanCodeExtension ext, final Path projectRoot) {
        final String baseRepoUrl = ext.getRepositoryUrl().get();
        if (baseRepoUrl.isBlank()) {
            return "";
        }
        final String modulePath = getProject().getRootDir().toPath().relativize(projectRoot).toString();
        return baseRepoUrl + "/blob/main" + (modulePath.isEmpty() ? "" : "/" + modulePath);
    }

    private void logReport(final AggregatedReport report, final Path baselineFile, final Path htmlReport)
            throws Exception {
        final Map<HeuristicCode, BaselineManager.Delta> deltas = Files.exists(baselineFile)
                ? BaselineManager.computeDeltas(report, baselineFile)
                : Map.of();
        getLogger().lifecycle(BuildOutputFormatter.format(report, deltas));
        getLogger().lifecycle("\n  Report: file://" + htmlReport.toAbsolutePath());
    }

    private AggregatedReport filterDisabledRecipes(final AggregatedReport report,
                                                   final Set<String> disabledRecipes) {
        if (disabledRecipes.isEmpty()) {
            return report;
        }
        final List<Finding> filtered = report.findings().stream()
                .filter(f -> !disabledRecipes.contains(f.code().name()))
                .toList();
        return withFindings(report, filtered);
    }

    private AggregatedReport filterSuppressions(final AggregatedReport report,
                                                final Path projectRoot,
                                                final PackageSuppression packageSuppression) {
        final SuppressionIndex index = SuppressionIndex.build(projectRoot.resolve(MAIN_SOURCES));
        final FindingFilter.Result result = FindingFilter.apply(report.findings(), index, packageSuppression);
        return withFindings(report, result.findings());
    }

    private static AggregatedReport withFindings(final AggregatedReport report, final List<Finding> findings) {
        return new AggregatedReport(
                findings,
                report.coveredCodes(),
                report.generatedAt(),
                report.projectName(),
                report.projectVersion());
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
