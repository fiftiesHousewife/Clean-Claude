package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.adapters.*;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewFindingSource;
import org.fiftieshousewife.cleancode.core.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AnalyseTask extends DefaultTask {

    @TaskAction
    public void analyse() throws Exception {
        final Path projectRoot = getProject().getProjectDir().toPath();
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportsDir = buildDir.resolve("reports");

        final CleanCodeExtension ext = getProject().getExtensions().getByType(CleanCodeExtension.class);
        final RecipeThresholds thresholds = ext.buildRecipeThresholds();
        final String anthropicApiKey = resolveApiKey();
        final ClaudeReviewConfig claudeConfig = ext.buildClaudeReviewConfig(anthropicApiKey);
        final Set<String> disabledRecipes = Set.copyOf(ext.getDisabledRecipes().get());

        final List<String> dependencies = getProject().getConfigurations().stream()
                .filter(c -> "runtimeClasspath".equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();

        final ProjectContext context = new ProjectContext(
                projectRoot,
                getProject().getName(),
                getProject().getVersion().toString(),
                "21",
                List.of(projectRoot.resolve("src/main/java")),
                List.of(projectRoot.resolve("src/test/java")),
                buildDir,
                reportsDir,
                dependencies);

        final List<FindingSource> sources = List.of(
                new PmdFindingSource(),
                new CheckstyleFindingSource(),
                new SpotBugsFindingSource(),
                new CpdFindingSource(),
                new JacocoFindingSource(),
                new SurefireFindingSource(),
                new DependencyUpdatesFindingSource(),
                new OpenRewriteFindingSource(thresholds),
                new ClaudeReviewFindingSource(claudeConfig));

        final AggregatedReport fullReport = FindingAggregator.aggregate(sources, context);
        final AggregatedReport report = filterDisabledRecipes(fullReport, disabledRecipes);

        final String baseRepoUrl = ext.getRepositoryUrl().get();
        final String modulePath = getProject().getRootDir().toPath().relativize(projectRoot).toString();
        final String repositoryUrl = baseRepoUrl.isBlank() ? ""
                : baseRepoUrl + (modulePath.isEmpty() ? "" : "/" + modulePath);
        final Path outputDir = buildDir.resolve("reports/clean-code");
        final Path htmlReport = outputDir.resolve("findings.html");
        JsonReportWriter.write(report, outputDir.resolve("findings.json"));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl);

        final Path baselineFile = projectRoot.resolve("clean-code-baseline.json");
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
        return new AggregatedReport(
                filtered,
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
