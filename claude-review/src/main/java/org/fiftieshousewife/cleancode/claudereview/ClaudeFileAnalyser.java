package org.fiftieshousewife.cleancode.claudereview;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import org.fiftieshousewife.cleancode.core.Finding;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ClaudeFileAnalyser {

    private static final Logger LOG = Logger.getLogger(ClaudeFileAnalyser.class.getName());
    private static final long MAX_TOKENS = 4096L;

    private final AnthropicClient client;
    private final String model;
    private final ClaudeFindingParser parser;

    ClaudeFileAnalyser(AnthropicClient client, String model, ClaudeFindingParser parser) {
        this.client = client;
        this.model = model;
        this.parser = parser;
    }

    List<Finding> analyse(AnalysisRequest request) {
        try {
            final String responseText = callApi(request);
            return parser.parse(responseText, request.relativePath());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Claude review failed for " + request.relativePath() + ": " + e.getMessage());
            return List.of();
        }
    }

    private String callApi(AnalysisRequest request) {
        final String userPrompt = "Assess this Java file for violations of: %s\n\nFile: %s\n\n%s"
                .formatted(request.codesKey(), request.relativePath(), addLineNumbers(request.content()));

        final MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(request.systemPrompt())
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                .addUserMessage(userPrompt)
                .build();

        final Message response = client.messages().create(params);
        return response.content().stream()
                .filter(ContentBlock::isText)
                .map(block -> block.text().orElseThrow().text())
                .collect(Collectors.joining());
    }

    private static String addLineNumbers(String content) {
        final String[] lines = content.split("\n", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append(": ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }
}
