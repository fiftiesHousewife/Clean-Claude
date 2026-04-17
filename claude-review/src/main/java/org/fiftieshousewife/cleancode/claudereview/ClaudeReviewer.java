package org.fiftieshousewife.cleancode.claudereview;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ClaudeReviewer {

    private static final Logger LOG = Logger.getLogger(ClaudeReviewer.class.getName());
    private static final long MAX_TOKENS = 4096L;

    private final AnthropicClient client;
    private final String systemPrompt;
    private final String model;
    private final String codesKey;

    ClaudeReviewer(final ClaudeReviewConfig config, final String codesKey) {
        this.client = AnthropicOkHttpClient.builder().apiKey(config.apiKey()).build();
        this.systemPrompt = loadSystemPrompt();
        this.model = config.model();
        this.codesKey = codesKey;
    }

    String review(final String content, final String relativePath) {
        try {
            final Message response = client.messages().create(buildParams(content, relativePath));
            return response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(block -> block.text().orElseThrow().text())
                    .collect(Collectors.joining());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Claude review failed for " + relativePath + ": " + e.getMessage());
            return "";
        }
    }

    private MessageCreateParams buildParams(final String content, final String relativePath) {
        final String userPrompt = "Assess this Java file for violations of: %s\n\nFile: %s\n\n%s"
                .formatted(codesKey, relativePath, addLineNumbers(content));
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(systemPrompt)
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                .addUserMessage(userPrompt)
                .build();
    }

    private static String addLineNumbers(final String content) {
        final String[] lines = content.split("\n", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append(": ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    private static String loadSystemPrompt() {
        try (InputStream is = ClaudeReviewer.class.getResourceAsStream("/claude-review-system.txt")) {
            if (is == null) {
                throw new IllegalStateException("System prompt resource not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system prompt", e);
        }
    }
}
