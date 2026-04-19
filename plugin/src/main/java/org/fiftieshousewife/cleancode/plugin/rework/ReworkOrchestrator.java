package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.Finding;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Java API for reworking Java classes via a single {@code claude -p}
 * session. Supports one or many target files — in a batched session
 * the agent reads each file lazily, refactors all of them, and emits
 * one JSON summary covering every action.
 */
public final class ReworkOrchestrator {

    private static final Duration DEFAULT_AGENT_TIMEOUT = Duration.ofMinutes(15);

    /**
     * Extra knobs threaded into a single {@link #reworkClasses} call.
     * Default ({@link #none()}) is "no re-analysis, no feedback retries"
     * — the pre-2026-04 behaviour. Use {@link Options#withFeedbackLoop}
     * to enable the post-agent feedback loop; the HARNESS variant
     * additionally uses {@code reAnalyser} between its recipe pass and
     * the agent call to regenerate findings against the post-recipe state.
     */
    public record Options(Supplier<AggregatedReport> reAnalyser, int maxRetries, Path moduleProjectDir) {
        public static Options none() {
            return new Options(null, 0, null);
        }

        public static Options withFeedbackLoop(final Supplier<AggregatedReport> reAnalyser,
                                               final int maxRetries, final Path moduleProjectDir) {
            return new Options(reAnalyser, maxRetries, moduleProjectDir);
        }
    }

    private final AgentRunner agentRunner;
    private final Duration agentTimeout;

    public ReworkOrchestrator() {
        this(new DefaultAgentRunner(), DEFAULT_AGENT_TIMEOUT);
    }

    public ReworkOrchestrator(final AgentRunner agentRunner, final Duration agentTimeout) {
        this.agentRunner = agentRunner;
        this.agentTimeout = agentTimeout;
    }

    public ReworkReport reworkClass(final Path file, final Path projectRoot,
                                    final AggregatedReport report, final ReworkMode mode)
            throws ReworkException {
        return reworkClass(file, projectRoot, report, mode, RunVariant.MCP_RECIPES);
    }

    public ReworkReport reworkClass(final Path file, final Path projectRoot,
                                    final AggregatedReport report, final ReworkMode mode,
                                    final RunVariant variant)
            throws ReworkException {
        return reworkClasses(List.of(file), projectRoot, report, mode, variant);
    }

    public ReworkReport reworkClasses(final List<Path> files, final Path projectRoot,
                                      final AggregatedReport report, final ReworkMode mode,
                                      final RunVariant variant)
            throws ReworkException {
        return reworkClasses(files, projectRoot, report, mode, variant, Options.none());
    }

    public ReworkReport reworkClasses(final List<Path> files, final Path projectRoot,
                                      final AggregatedReport report, final ReworkMode mode,
                                      final RunVariant variant, final Options options)
            throws ReworkException {
        final List<FileTarget> targets = toTargets(files, projectRoot, report);
        return switch (mode) {
            case SUGGEST_ONLY -> suggestOnly(targets);
            case AGENT_DRIVEN -> agentDriven(targets, files, projectRoot, variant, options);
        };
    }

    private static List<FileTarget> toTargets(final List<Path> files, final Path projectRoot,
                                              final AggregatedReport report) {
        final List<FileTarget> targets = new ArrayList<>();
        files.forEach(file -> {
            final String relative = projectRoot.relativize(file).toString();
            targets.add(new FileTarget(file, relative,
                    SuggestionDetector.suggestionsFor(report, relative)));
        });
        return targets;
    }

    private ReworkReport suggestOnly(final List<FileTarget> targets) {
        final List<Suggestion> aggregated = aggregateSuggestions(targets);
        final String body = CommitMessageFormatter.format(
                List.of(), List.of(), aggregated, Optional.empty());
        return new ReworkReport(paths(targets), ReworkMode.SUGGEST_ONLY, aggregated,
                List.of(), List.of(), Optional.empty(), body);
    }

    private ReworkReport agentDriven(final List<FileTarget> initialTargets, final List<Path> files,
                                     final Path projectRoot, final RunVariant variant,
                                     final Options options)
            throws ReworkException {
        final HarnessRecipePass.PassSummary harnessPass = maybeRunHarnessRecipes(initialTargets, variant);
        if (variant == RunVariant.RECIPES_ONLY) {
            final List<Suggestion> aggregated = aggregateSuggestions(initialTargets);
            final List<AgentAction> recipeActions = prefixHarnessActions(harnessPass, List.of());
            final String body = CommitMessageFormatter.format(
                    recipeActions, List.of(), aggregated, Optional.empty());
            return new ReworkReport(paths(initialTargets), ReworkMode.AGENT_DRIVEN, aggregated,
                    recipeActions, List.of(), Optional.empty(), body);
        }
        final List<FileTarget> promptTargets = refreshTargetsAfterRecipePass(
                initialTargets, files, projectRoot, variant, options);

        final AgentResult firstResult = runAgent(PromptBuilder.build(promptTargets, variant, false));
        final AgentResponseParser.Parsed firstParsed = AgentResponseParser.parse(firstResult.text());

        final RetryOutcome retry = runFeedbackRetries(files, projectRoot, variant,
                options, promptTargets, firstResult, firstParsed);

        final List<AgentAction> allActions = new ArrayList<>(firstParsed.actions());
        allActions.addAll(retry.extraActions());
        final List<AgentRejection> allRejected = new ArrayList<>(firstParsed.rejected());
        allRejected.addAll(retry.extraRejected());
        final Optional<AgentUsage> combinedUsage = combineUsage(firstResult.usage(), retry.extraUsage());

        final List<Suggestion> aggregated = aggregateSuggestions(promptTargets);
        final List<AgentAction> combined = prefixHarnessActions(harnessPass, allActions);
        final String body = CommitMessageFormatter.format(combined, allRejected, aggregated, combinedUsage);
        return new ReworkReport(paths(initialTargets), ReworkMode.AGENT_DRIVEN, aggregated,
                combined, allRejected, combinedUsage, body);
    }

    private record RetryOutcome(List<AgentAction> extraActions,
                                List<AgentRejection> extraRejected,
                                Optional<AgentUsage> extraUsage) {
        static RetryOutcome none() {
            return new RetryOutcome(List.of(), List.of(), Optional.empty());
        }
    }

    private RetryOutcome runFeedbackRetries(final List<Path> files, final Path projectRoot,
                                            final RunVariant variant, final Options options,
                                            final List<FileTarget> preAgentTargets,
                                            final AgentResult firstResult,
                                            final AgentResponseParser.Parsed firstParsed)
            throws ReworkException {
        if (options.maxRetries() <= 0 || options.reAnalyser() == null || options.moduleProjectDir() == null) {
            return RetryOutcome.none();
        }
        final List<Finding> preAgentFindings = flattenSuggestionsToFindings(preAgentTargets);
        final Set<Path> targetSet = new HashSet<>(files);
        final List<AgentAction> extraActions = new ArrayList<>();
        final List<AgentRejection> extraRejected = new ArrayList<>();
        Optional<AgentUsage> extraUsage = Optional.empty();

        List<Finding> priorFindings = preAgentFindings;
        for (int attempt = 0; attempt < options.maxRetries(); attempt++) {
            // HARNESS retry: rerun the deterministic recipes on the agent's output
            // before spending another agent turn. If the recipes clear every new
            // finding, we skip the agent call entirely.
            final HarnessRecipePass.PassSummary retryRecipes = runRecipesOnRetry(files, variant);
            if (!retryRecipes.recipeNamesByFile().isEmpty()) {
                extraActions.addAll(0, harnessPassActions(retryRecipes));
            }
            final AggregatedReport fresh = invokeAnalyser(options);
            final List<Finding> introduced = FindingsSnapshot.introducedFindings(
                    priorFindings, fresh.findings(), targetSet, options.moduleProjectDir());
            if (introduced.isEmpty()) {
                break;
            }
            final List<FileTarget> retryTargets = buildRetryTargets(
                    files, projectRoot, introduced, targetSet, options.moduleProjectDir());
            if (retryTargets.stream().allMatch(t -> t.suggestions().isEmpty())) {
                break;
            }
            final AgentResult retryResult = runAgent(PromptBuilder.build(retryTargets, variant, true));
            final AgentResponseParser.Parsed retryParsed = AgentResponseParser.parse(retryResult.text());
            extraActions.addAll(retryParsed.actions());
            extraRejected.addAll(retryParsed.rejected());
            extraUsage = combineUsage(extraUsage, retryResult.usage());
            priorFindings = fresh.findings();
        }
        return new RetryOutcome(extraActions, extraRejected, extraUsage);
    }

    private static HarnessRecipePass.PassSummary runRecipesOnRetry(final List<Path> files,
                                                                   final RunVariant variant)
            throws ReworkException {
        if (variant != RunVariant.HARNESS_RECIPES_THEN_AGENT) {
            return new HarnessRecipePass.PassSummary(Map.of());
        }
        try {
            return HarnessRecipePass.apply(files);
        } catch (IOException e) {
            throw new ReworkException("harness retry recipe pass failed: " + e.getMessage(), e);
        }
    }

    private static List<Finding> flattenSuggestionsToFindings(final List<FileTarget> targets) {
        // The agent-initial findings are embedded in each target's suggestions; we only
        // need the (code, absolute-path, line) key for the introduced-vs-same diff.
        final List<Finding> findings = new ArrayList<>();
        for (final FileTarget target : targets) {
            target.suggestions().forEach(s -> findings.add(Finding.at(
                    s.code(), target.absolutePath().toString(),
                    s.line(), s.line(), s.message(),
                    org.fiftieshousewife.cleancode.core.Severity.WARNING,
                    org.fiftieshousewife.cleancode.core.Confidence.HIGH,
                    "rework-seed", "seed")));
        }
        return findings;
    }

    private static List<FileTarget> buildRetryTargets(final List<Path> files, final Path projectRoot,
                                                      final List<Finding> introduced,
                                                      final Set<Path> targetSet,
                                                      final Path moduleProjectDir) {
        final Map<Path, List<Suggestion>> byFile = new java.util.HashMap<>();
        for (final Finding finding : introduced) {
            if (finding.sourceFile() == null) {
                continue;
            }
            final Path absolute = absolutise(finding.sourceFile(), moduleProjectDir);
            if (!targetSet.contains(absolute)) {
                continue;
            }
            byFile.computeIfAbsent(absolute, __ -> new ArrayList<>())
                    .add(new Suggestion(finding.code(), finding.startLine(), finding.message()));
        }
        final List<FileTarget> retryTargets = new ArrayList<>();
        for (final Path file : files) {
            if (!byFile.containsKey(file)) {
                continue;
            }
            final String relative = projectRoot.relativize(file).toString();
            retryTargets.add(new FileTarget(file, relative, byFile.get(file)));
        }
        return retryTargets;
    }

    private static Path absolutise(final String sourceFile, final Path moduleProjectDir) {
        final Path path = Path.of(sourceFile);
        return path.isAbsolute() ? path.normalize() : moduleProjectDir.resolve(path).normalize();
    }

    private AggregatedReport invokeAnalyser(final Options options) throws ReworkException {
        try {
            return options.reAnalyser().get();
        } catch (RuntimeException e) {
            throw new ReworkException("post-agent re-analysis failed: " + e.getMessage(), e);
        }
    }

    private static Optional<AgentUsage> combineUsage(final Optional<AgentUsage> left,
                                                     final Optional<AgentUsage> right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        final AgentUsage a = left.get();
        final AgentUsage b = right.get();
        return Optional.of(new AgentUsage(
                a.inputTokens() + b.inputTokens(),
                a.outputTokens() + b.outputTokens(),
                a.cacheCreationInputTokens() + b.cacheCreationInputTokens(),
                a.cacheReadInputTokens() + b.cacheReadInputTokens(),
                a.durationMs() + b.durationMs(),
                a.numTurns() + b.numTurns(),
                a.totalCostUsd() + b.totalCostUsd()));
    }

    private static HarnessRecipePass.PassSummary maybeRunHarnessRecipes(
            final List<FileTarget> targets, final RunVariant variant) throws ReworkException {
        if (variant != RunVariant.HARNESS_RECIPES_THEN_AGENT
                && variant != RunVariant.RECIPES_ONLY) {
            return new HarnessRecipePass.PassSummary(Map.of());
        }
        try {
            return HarnessRecipePass.apply(paths(targets));
        } catch (IOException e) {
            throw new ReworkException("harness recipe pass failed: " + e.getMessage(), e);
        }
    }

    private static List<FileTarget> refreshTargetsAfterRecipePass(
            final List<FileTarget> originalTargets, final List<Path> files, final Path projectRoot,
            final RunVariant variant, final Options options)
            throws ReworkException {
        if (variant != RunVariant.HARNESS_RECIPES_THEN_AGENT || options.reAnalyser() == null) {
            return originalTargets;
        }
        try {
            final AggregatedReport fresh = options.reAnalyser().get();
            return toTargets(files, projectRoot, fresh);
        } catch (RuntimeException e) {
            throw new ReworkException(
                    "post-recipe re-analysis failed: " + e.getMessage(), e);
        }
    }

    private static List<AgentAction> prefixHarnessActions(final HarnessRecipePass.PassSummary pass,
                                                          final List<AgentAction> agentActions) {
        if (pass.recipeNamesByFile().isEmpty()) {
            return agentActions;
        }
        final List<AgentAction> combined = new ArrayList<>(harnessPassActions(pass));
        combined.addAll(agentActions);
        return combined;
    }

    private static List<AgentAction> harnessPassActions(final HarnessRecipePass.PassSummary pass) {
        final List<AgentAction> actions = new ArrayList<>();
        pass.recipeNamesByFile().forEach((file, recipeNames) -> actions.add(new AgentAction(
                "HarnessRecipePass",
                Map.of("file", file.getFileName().toString(),
                        "recipes", String.join(", ", recipeNames)),
                "Harness applied " + String.join(", ", recipeNames) + " deterministically "
                        + "before handing off to the agent")));
        return actions;
    }

    private AgentResult runAgent(final String prompt) throws ReworkException {
        try {
            return agentRunner.run(prompt, agentTimeout);
        } catch (AgentRunner.AgentRunnerException e) {
            throw new ReworkException("agent invocation failed: " + e.getMessage(), e);
        }
    }

    private static List<Suggestion> aggregateSuggestions(final List<FileTarget> targets) {
        final List<Suggestion> all = new ArrayList<>();
        targets.forEach(target -> all.addAll(target.suggestions()));
        return all;
    }

    private static List<Path> paths(final List<FileTarget> targets) {
        return targets.stream().map(FileTarget::absolutePath).toList();
    }

    public static final class ReworkException extends Exception {
        private static final long serialVersionUID = 1L;

        public ReworkException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
