package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.core.AggregatedReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Java API for reworking a single class. Orchestrates the three pieces
 * that matter: {@link SuggestionDetector} (what does the plugin know
 * about this file?), {@link AgentRunner} (does the agent want a turn?),
 * and {@link AgentResponseParser} + {@link CommitMessageFormatter}
 * (what do we return to the caller for the commit message body?).
 *
 * <p>Invoked from the {@code :plugin:reworkClass} Gradle task so users
 * can drive it with one command; the task is the only public surface.
 * There is intentionally no per-feature shell script.
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
        return reworkClass(file, projectRoot, report, mode, true);
    }

    public ReworkReport reworkClass(final Path file, final Path projectRoot,
                                    final AggregatedReport report, final ReworkMode mode,
                                    final boolean includeRecipeTools)
            throws ReworkException {
        final String relativePath = projectRoot.relativize(file).toString();
        final List<Suggestion> suggestions = SuggestionDetector.suggestionsFor(report, relativePath);
        return switch (mode) {
            case SUGGEST_ONLY -> suggestOnly(file, suggestions);
            case AGENT_DRIVEN -> agentDriven(file, relativePath, suggestions, includeRecipeTools);
        };
    }

    private ReworkReport suggestOnly(final Path file, final List<Suggestion> suggestions) {
        final String body = CommitMessageFormatter.format(List.of(), List.of(), suggestions);
        return new ReworkReport(file, ReworkMode.SUGGEST_ONLY, suggestions,
                List.of(), List.of(), body);
    }

    private ReworkReport agentDriven(final Path file, final String relativePath,
                                     final List<Suggestion> suggestions,
                                     final boolean includeRecipeTools) throws ReworkException {
        final String contents = readContents(file);
        final String prompt = PromptBuilder.build(relativePath, contents, suggestions, includeRecipeTools);
        final String stdout = runAgent(prompt);
        final AgentResponseParser.Parsed parsed = AgentResponseParser.parse(stdout);
        final String body = CommitMessageFormatter.format(
                parsed.actions(), parsed.rejected(), suggestions);
        return new ReworkReport(file, ReworkMode.AGENT_DRIVEN, suggestions,
                parsed.actions(), parsed.rejected(), body);
    }

    private String runAgent(final String prompt) throws ReworkException {
        try {
            return agentRunner.run(prompt, agentTimeout);
        } catch (AgentRunner.AgentRunnerException e) {
            throw new ReworkException("agent invocation failed: " + e.getMessage(), e);
        }
    }

    private static String readContents(final Path file) throws ReworkException {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new ReworkException("could not read " + file, e);
        }
    }

    public static final class ReworkException extends Exception {
        private static final long serialVersionUID = 1L;
        public ReworkException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
