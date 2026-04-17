package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.adapters.CheckstyleFindingSource;
import org.fiftieshousewife.cleancode.adapters.CpdFindingSource;
import org.fiftieshousewife.cleancode.adapters.DependencyUpdatesFindingSource;
import org.fiftieshousewife.cleancode.adapters.JacocoFindingSource;
import org.fiftieshousewife.cleancode.adapters.OpenRewriteFindingSource;
import org.fiftieshousewife.cleancode.adapters.PmdFindingSource;
import org.fiftieshousewife.cleancode.adapters.SpotBugsFindingSource;
import org.fiftieshousewife.cleancode.adapters.SurefireFindingSource;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewFindingSource;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.FindingAggregator;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.PackageSuppression;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class AnalyseTask extends DefaultTask {

    private static final String REPORTS_SUBDIR = "reports";
    private static final String CLEAN_CODE_REPORTS_SUBDIR = "reports/clean-code";
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
        final AggregatedReport report = new AnalyseFilters(projectRoot.resolve(MAIN_SOURCES))
                .apply(rawReport, Set.copyOf(ext.getDisabledRecipes().get()),
                        PackageSuppression.of(ext.getPackageSuppressions().get()));

        final Path outputDir = buildDir.resolve(CLEAN_CODE_REPORTS_SUBDIR);
        final Path htmlReport = outputDir.resolve(FINDINGS_HTML);
        final AnalyseOutputs outputs = new AnalyseOutputs(
                getLogger(), ext.getRepositoryUrl().get(), getProject().getRootDir().toPath(), projectRoot);
        outputs.writeReports(report, outputDir, htmlReport);
        outputs.logReport(report, projectRoot.resolve(BASELINE_FILE), htmlReport);
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

    private String resolveApiKey() {
        final Object prop = getProject().findProperty("ANTHROPIC_API_KEY");
        if (prop != null && !prop.toString().isBlank()) {
            return prop.toString();
        }
        final String env = System.getenv("ANTHROPIC_API_KEY");
        return env != null ? env : "";
    }
}
