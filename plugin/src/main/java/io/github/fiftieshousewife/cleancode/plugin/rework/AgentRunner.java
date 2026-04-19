package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.time.Duration;

/**
 * Drives {@code claude -p} (or an equivalent agent runtime) from Java.
 * Injected into {@link ReworkOrchestrator} so tests can substitute a
 * deterministic fake without needing the CLI installed. The default
 * production implementation is {@link DefaultAgentRunner}.
 */
public interface AgentRunner {

    AgentResult run(String prompt, Duration timeout) throws AgentRunnerException;

    final class AgentRunnerException extends Exception {
        private static final long serialVersionUID = 1L;
        public AgentRunnerException(final String message, final Throwable cause) {
            super(message, cause);
        }
        public AgentRunnerException(final String message) {
            super(message);
        }
    }
}
