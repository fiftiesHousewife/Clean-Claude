package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.adapters.CheckstyleFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.CpdFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.DependencyUpdatesFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.JacocoFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.OpenRewriteFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.PmdFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.SpotBugsFindingSource;
import io.github.fiftieshousewife.cleancode.adapters.SurefireFindingSource;
import io.github.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import io.github.fiftieshousewife.cleancode.claudereview.ClaudeReviewFindingSource;
import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.FindingAggregator;
import io.github.fiftieshousewife.cleancode.core.FindingFilter;
import io.github.fiftieshousewife.cleancode.core.FindingSource;
import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import io.github.fiftieshousewife.cleancode.core.PackageSuppression;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
import io.github.fiftieshousewife.cleancode.core.RecipeThresholds;
import io.github.fiftieshousewife.cleancode.core.SuppressionIndex;
import org.gradle.api.Project;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Runs the full clean-code analysis in-process against a Gradle project
 * and returns the filtered {@link AggregatedReport}. {@link AnalyseTask}
 * calls this once per build; {@link ReworkCompareTask} calls it once per
 * variant (after the agent edits the sandbox, before the files are
 * restored) so it can count findings fixed and introduced per variant.
 * Pure in-memory — does not write {@code findings.json} or any report
 * file, so it's safe to invoke from within another running task.
 */
public final class SandboxAnalysis {

    private SandboxAnalysis() {}

    public static AggregatedReport analyse(final Project project) throws FindingSourceException {
        final Path projectRoot = project.getProjectDir().toPath();
        final Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportsDir = buildDir.resolve("reports");

        final CleanCodeExtension ext = project.getExtensions().getByType(CleanCodeExtension.class);
        final RecipeThresholds thresholds = ext.buildRecipeThresholds();
        final String anthropicApiKey = resolveApiKey(project);
        final ClaudeReviewConfig claudeConfig = ext.buildClaudeReviewConfig(anthropicApiKey);
        final Set<String> disabledRecipes = Set.copyOf(ext.getDisabledRecipes().get());
        final PackageSuppression packageSuppression = PackageSuppression.of(ext.getPackageSuppressions().get());

        final List<String> dependencies = project.getConfigurations().stream()
                .filter(c -> "runtimeClasspath".equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();

        final ProjectContext context = new ProjectContext(
                projectRoot,
                project.getName(),
                project.getVersion().toString(),
                "21",
                List.of(projectRoot.resolve("src/main/java")),
                List.of(projectRoot.resolve("src/test/java")),
                buildDir,
                reportsDir,
                dependencies);

        final List<FindingSource> sources = List.of(
                new PmdFindingSource(),
                new CheckstyleFindingSource(thresholds),
                new SpotBugsFindingSource(),
                new CpdFindingSource(),
                new JacocoFindingSource(),
                new SurefireFindingSource(),
                new DependencyUpdatesFindingSource(),
                new OpenRewriteFindingSource(thresholds),
                new ClaudeReviewFindingSource(claudeConfig));

        final AggregatedReport fullReport = FindingAggregator.aggregate(sources, context);
        final AggregatedReport afterDisabled = filterDisabledRecipes(fullReport, disabledRecipes);
        return filterSuppressions(afterDisabled, projectRoot, packageSuppression);
    }

    private static AggregatedReport filterDisabledRecipes(final AggregatedReport report,
                                                          final Set<String> disabledRecipes) {
        if (disabledRecipes.isEmpty()) {
            return report;
        }
        final List<Finding> filtered = report.findings().stream()
                .filter(f -> !disabledRecipes.contains(f.code().name()))
                .toList();
        return withFindings(report, filtered);
    }

    private static AggregatedReport filterSuppressions(final AggregatedReport report,
                                                       final Path projectRoot,
                                                       final PackageSuppression packageSuppression) {
        final SuppressionIndex index = SuppressionIndex.build(projectRoot.resolve("src/main/java"));
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

    private static String resolveApiKey(final Project project) {
        final Object prop = project.findProperty("ANTHROPIC_API_KEY");
        if (prop != null && !prop.toString().isBlank()) {
            return prop.toString();
        }
        final String env = System.getenv("ANTHROPIC_API_KEY");
        return env != null ? env : "";
    }
}
