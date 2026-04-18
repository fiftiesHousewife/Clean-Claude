package org.fiftieshousewife.cleancode.plugin.rework;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Production {@link AgentRunner} that spawns the {@code claude} CLI via
 * {@link ProcessBuilder}. Writes the prompt to the CLI's stdin (avoids
 * command-line length limits for large file bodies) and reads the
 * merged stdout+stderr back as a string. The CLI is invoked without
 * {@code --output-format json} so the agent's response arrives as
 * plain text — {@link AgentResponseParser} finds the JSON schema
 * object inside.
 */
public final class DefaultAgentRunner implements AgentRunner {

    private static final String CLAUDE_COMMAND = "claude";
    private static final String PRINT_FLAG = "-p";

    @Override
    public String run(final String prompt, final Duration timeout) throws AgentRunnerException {
        final ProcessBuilder builder = new ProcessBuilder(CLAUDE_COMMAND, PRINT_FLAG)
                .redirectErrorStream(true);
        final Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AgentRunnerException("failed to start `" + CLAUDE_COMMAND
                    + "` — is the CLI on PATH?", e);
        }
        writePrompt(process, prompt);
        return waitForOutput(process, timeout);
    }

    private static void writePrompt(final Process process, final String prompt) throws AgentRunnerException {
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException e) {
            process.destroyForcibly();
            throw new AgentRunnerException("failed writing prompt to claude stdin", e);
        }
    }

    private static String waitForOutput(final Process process, final Duration timeout) throws AgentRunnerException {
        final boolean completed;
        try {
            completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new AgentRunnerException("interrupted waiting for claude", e);
        }
        if (!completed) {
            process.destroyForcibly();
            throw new AgentRunnerException("claude timed out after " + timeout);
        }
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AgentRunnerException("failed reading claude stdout", e);
        }
    }
}
