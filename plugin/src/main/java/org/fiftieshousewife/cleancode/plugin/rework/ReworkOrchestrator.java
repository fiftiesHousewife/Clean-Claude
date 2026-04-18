package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.core.AggregatedReport;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Java API for reworking a single class. Orchestrates the three pieces
 * that matter: {@link SuggestionDetector} (what does the plugin know
 * about this file?), {@link AgentRunner} (does the agent want a turn?),
 * and {@link AgentResponseParser} + {@link CommitMessageFormatter}
 * (what do we return to the caller for the commit message body?).
 *
 * <p>Invoked from the {@code :reworkClass} Gradle task so users can
 * drive it with one command; the task is the only public surface.
 * There is intentionally no per-feature shell script.
 *
 * <p>The agent reads the target file itself via its Read tool — we
 * hand it only the relative path and the structured findings, not the
 * file contents.
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
        final String relativePath = projectRoot.relativize(file).toString();
        final List<Suggestion> suggestions = SuggestionDetector.suggestionsFor(report, relativePath);
        return switch (mode) {
            case SUGGEST_ONLY -> suggestOnly(file, suggestions);
            case AGENT_DRIVEN -> agentDriven(file, relativePath, suggestions, variant);
        };
    }

    private ReworkReport suggestOnly(final Path file, final List<Suggestion> suggestions) {
        final String body = CommitMessageFormatter.format(
                List.of(), List.of(), suggestions, Optional.empty());
        return new ReworkReport(file, ReworkMode.SUGGEST_ONLY, suggestions,
                List.of(), List.of(), Optional.empty(), body);
    }

    private ReworkReport agentDriven(final Path file, final String relativePath,
                                     final List<Suggestion> suggestions,
                                     final RunVariant variant) throws ReworkException {
        final String prompt = PromptBuilder.build(relativePath, suggestions, variant);
        final AgentResult result = runAgent(prompt);
        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(result.text());
        final String body = CommitMessageFormatter.format(
                parsed.actions(), parsed.rejected(), suggestions, result.usage());
        return new ReworkReport(file, ReworkMode.AGENT_DRIVEN, suggestions,
                parsed.actions(), parsed.rejected(), result.usage(), body);
    }

    private AgentResult runAgent(final String prompt) throws ReworkException {
        try {
            return agentRunner.run(prompt, agentTimeout);
        } catch (AgentRunner.AgentRunnerException e) {
            throw new ReworkException("agent invocation failed: " + e.getMessage(), e);
        }
    }

    public static final class ReworkException extends Exception {
        private static final long serialVersionUID = 1L;
        public ReworkException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
