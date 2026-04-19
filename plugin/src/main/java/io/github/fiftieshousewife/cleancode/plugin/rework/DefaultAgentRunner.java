package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Production {@link AgentRunner}. Spawns {@code claude -p --output-format
 * stream-json} so the agent's text, tool calls, and tool results arrive as
 * they happen — the {@link #DefaultAgentRunner(Consumer) progress} callback
 * gets a one-line summary for each event, so the calling task can log live
 * progress through Gradle's lifecycle instead of showing a silent 15-minute
 * task. The terminal {@code result} event's text is fed to
 * {@link AgentResponseParser} and its usage becomes {@link AgentUsage}.
 */
public final class DefaultAgentRunner implements AgentRunner {

    private static final String CLAUDE_COMMAND = "claude";
    private static final String PRINT_FLAG = "-p";
    private static final String FORMAT_FLAG = "--output-format";
    private static final String STREAM_JSON_FORMAT = "stream-json";
    private static final String VERBOSE_FLAG = "--verbose";
    private static final int TEXT_PREVIEW_LIMIT = 120;
    private static final long READER_JOIN_MILLIS = 5_000;

    private final Consumer<String> progress;

    public DefaultAgentRunner() {
        this(line -> { });
    }

    public DefaultAgentRunner(final Consumer<String> progress) {
        this.progress = progress;
    }

    @Override
    public AgentResult run(final String prompt, final Duration timeout) throws AgentRunnerException {
        final Process process = spawn();
        writePrompt(process, prompt);
        final Collector collector = new Collector();
        final Thread reader = startReaderThread(process, collector);
        awaitCompletion(process, timeout);
        joinReader(reader);
        return collector.result();
    }

    private static Process spawn() throws AgentRunnerException {
        try {
            return new ProcessBuilder(CLAUDE_COMMAND, PRINT_FLAG,
                    FORMAT_FLAG, STREAM_JSON_FORMAT, VERBOSE_FLAG)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new AgentRunnerException("failed to start `" + CLAUDE_COMMAND
                    + "` — is the CLI on PATH?", e);
        }
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

    private Thread startReaderThread(final Process process, final Collector collector) {
        final Thread reader = new Thread(() -> readStream(process, collector), "claude-stream-reader");
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

    private void readStream(final Process process, final Collector collector) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                handleEvent(StreamJsonEventParser.parseLine(line), collector);
            }
        } catch (IOException ignored) {
            // stream closed — process exited; remaining events are lost which is fine
        }
    }

    private void handleEvent(final StreamEvent event, final Collector collector) {
        switch (event) {
            case StreamEvent.AssistantText t -> progress.accept("text: " + shorten(t.text()));
            case StreamEvent.ToolUse u -> progress.accept("tool: " + u.name()
                    + (u.inputSummary().isBlank() ? "" : " (" + u.inputSummary() + ")"));
            case StreamEvent.ToolResult ignored -> { }
            case StreamEvent.Result r -> collector.capture(r);
            case StreamEvent.Ignored ignored -> { }
        }
    }

    private static void awaitCompletion(final Process process, final Duration timeout) throws AgentRunnerException {
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
    }

    private static void joinReader(final Thread reader) {
        try {
            reader.join(READER_JOIN_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String shorten(final String text) {
        final String single = text.replace('\n', ' ').trim();
        return single.length() <= TEXT_PREVIEW_LIMIT
                ? single
                : single.substring(0, TEXT_PREVIEW_LIMIT) + "…";
    }

    private static final class Collector {
        private String text = "";
        private Optional<AgentUsage> usage = Optional.empty();

        void capture(final StreamEvent.Result event) {
            text = event.text();
            usage = event.usage();
        }

        AgentResult result() {
            return new AgentResult(text, usage);
        }
    }
}
