package org.fiftieshousewife.cleancode.plugin.rework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Production {@link AgentRunner} that spawns the {@code claude} CLI via
 * {@link ProcessBuilder} with {@code --output-format json} so both the
 * agent's text response and its token accounting come back. The inner
 * {@code result} field is what {@link AgentResponseParser} consumes;
 * the envelope's {@code usage} and {@code total_cost_usd} fields become
 * the {@link AgentUsage} returned alongside.
 */
public final class DefaultAgentRunner implements AgentRunner {

    private static final String CLAUDE_COMMAND = "claude";
    private static final String PRINT_FLAG = "-p";
    private static final String FORMAT_FLAG = "--output-format";
    private static final String JSON_FORMAT = "json";

    @Override
    public AgentResult run(final String prompt, final Duration timeout) throws AgentRunnerException {
        final ProcessBuilder builder = new ProcessBuilder(CLAUDE_COMMAND, PRINT_FLAG, FORMAT_FLAG, JSON_FORMAT);
        final Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AgentRunnerException("failed to start `" + CLAUDE_COMMAND
                    + "` — is the CLI on PATH?", e);
        }
        writePrompt(process, prompt);
        final String envelopeJson = waitForOutput(process, timeout);
        return parseEnvelope(envelopeJson);
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

    static AgentResult parseEnvelope(final String envelopeJson) {
        try {
            final JsonElement parsed = JsonParser.parseString(envelopeJson);
            if (!parsed.isJsonObject()) {
                return AgentResult.textOnly(envelopeJson);
            }
            final JsonObject envelope = parsed.getAsJsonObject();
            final String text = envelope.has("result") && envelope.get("result").isJsonPrimitive()
                    ? envelope.get("result").getAsString()
                    : envelopeJson;
            return new AgentResult(text, extractUsage(envelope));
        } catch (JsonParseException e) {
            return AgentResult.textOnly(envelopeJson);
        }
    }

    private static Optional<AgentUsage> extractUsage(final JsonObject envelope) {
        if (!envelope.has("usage") || !envelope.get("usage").isJsonObject()) {
            return Optional.empty();
        }
        final JsonObject usage = envelope.getAsJsonObject("usage");
        final double costUsd = envelope.has("total_cost_usd")
                && envelope.get("total_cost_usd").isJsonPrimitive()
                ? envelope.get("total_cost_usd").getAsDouble() : 0.0;
        return Optional.of(new AgentUsage(
                intOrZero(usage, "input_tokens"),
                intOrZero(usage, "output_tokens"),
                intOrZero(usage, "cache_creation_input_tokens"),
                intOrZero(usage, "cache_read_input_tokens"),
                costUsd));
    }

    private static int intOrZero(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsInt() : 0;
    }
}
