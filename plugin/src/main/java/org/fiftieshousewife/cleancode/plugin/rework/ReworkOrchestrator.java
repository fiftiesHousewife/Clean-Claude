package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.core.AggregatedReport;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Java API for reworking Java classes via a single {@code claude -p}
 * session. Supports one or many target files — in a batched session
 * the agent reads each file lazily, refactors all of them, and emits
 * one JSON summary covering every action.
 */
public final class ReworkOrchestrator {

    private static final Duration DEFAULT_AGENT_TIMEOUT = Duration.ofMinutes(15);

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
        final List<FileTarget> targets = toTargets(files, projectRoot, report);
        return switch (mode) {
            case SUGGEST_ONLY -> suggestOnly(targets);
            case AGENT_DRIVEN -> agentDriven(targets, variant);
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

    private ReworkReport agentDriven(final List<FileTarget> targets, final RunVariant variant)
            throws ReworkException {
        final String prompt = PromptBuilder.build(targets, variant);
        final AgentResult result = runAgent(prompt);
        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(result.text());
        final List<Suggestion> aggregated = aggregateSuggestions(targets);
        final String body = CommitMessageFormatter.format(
                parsed.actions(), parsed.rejected(), aggregated, result.usage());
        return new ReworkReport(paths(targets), ReworkMode.AGENT_DRIVEN, aggregated,
                parsed.actions(), parsed.rejected(), result.usage(), body);
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
        final List<Path> out = new ArrayList<>();
        targets.forEach(target -> out.add(target.absolutePath()));
        return out;
    }

    public static final class ReworkException extends Exception {
        private static final long serialVersionUID = 1L;
        public ReworkException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
