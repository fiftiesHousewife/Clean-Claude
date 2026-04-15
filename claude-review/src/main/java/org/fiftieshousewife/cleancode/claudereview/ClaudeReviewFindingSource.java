package org.fiftieshousewife.cleancode.claudereview;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClaudeReviewFindingSource implements FindingSource {

    private static final String TOOL = "claude-review";
    private static final Logger LOG = Logger.getLogger(ClaudeReviewFindingSource.class.getName());

    private final ClaudeReviewConfig config;

    public ClaudeReviewFindingSource(ClaudeReviewConfig config) {
        this.config = config;
    }

    @Override
    public String id() {
        return TOOL;
    }

    @Override
    public String displayName() {
        return "Claude Review";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return Collections.unmodifiableSet(config.enabledCodes());
    }

    @Override
    public boolean isAvailable(ProjectContext context) {
        return config.enabled() && System.getenv("ANTHROPIC_API_KEY") != null;
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null) {
            return List.of();
        }

        final AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        final String systemPrompt = loadSystemPrompt();
        final String codesKey = config.enabledCodes().stream()
                .map(HeuristicCode::name)
                .sorted()
                .collect(Collectors.joining(","));

        final List<Path> files = collectSourceFiles(context);
        final Path cacheDir = context.buildDir().resolve("claude-review-cache");
        final ReviewCache cache = ReviewCache.load(cacheDir);

        final List<Finding> allFindings = new ArrayList<>();
        int analysed = 0;

        for (final Path file : files) {
            if (analysed >= config.maxFilesPerRun()) {
                break;
            }

            try {
                final String content = Files.readString(file, StandardCharsets.UTF_8);
                final String relativePath = context.projectRoot().relativize(file).toString();
                final String hash = ReviewCache.hash(content, codesKey);

                final var cached = cache.lookup(hash);
                if (cached.isPresent()) {
                    allFindings.addAll(toCoreFindings(cached.get(), relativePath));
                    continue;
                }

                final List<Finding> findings = analyseFile(client, content, relativePath, systemPrompt, codesKey);
                final List<ReviewCache.CachedFinding> cachedFindings = toCachedFindings(findings);
                cache.store(hash, cachedFindings);
                allFindings.addAll(findings);
                analysed++;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to read file: " + file, e);
            }
        }

        try {
            cache.save(cacheDir);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save review cache", e);
        }

        return allFindings;
    }

    List<Finding> analyseFile(AnthropicClient client, String content, String relativePath,
                              String systemPrompt, String codesKey) {
        try {
            final String numberedContent = addLineNumbers(content);
            final String userPrompt = "Assess this Java file for violations of: %s\n\nFile: %s\n\n%s"
                    .formatted(codesKey, relativePath, numberedContent);

            final MessageCreateParams params = MessageCreateParams.builder()
                    .model(config.model())
                    .maxTokens(4096L)
                    .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                            .text(systemPrompt)
                            .cacheControl(CacheControlEphemeral.builder().build())
                            .build()))
                    .addUserMessage(userPrompt)
                    .build();

            final Message response = client.messages().create(params);
            final String responseText = response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(block -> block.text().orElseThrow().text())
                    .collect(Collectors.joining());

            return parseFindings(responseText, relativePath);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Claude review failed for " + relativePath + ": " + e.getMessage());
            return List.of();
        }
    }

    List<Finding> parseFindings(String json, String sourceFile) {
        try {
            final String cleaned = json.strip();
            final String toParse = cleaned.startsWith("[") ? cleaned
                    : extractJsonArray(cleaned);

            final JsonArray array = JsonParser.parseString(toParse).getAsJsonArray();
            final List<Finding> findings = new ArrayList<>();

            for (final JsonElement element : array) {
                final JsonObject obj = element.getAsJsonObject();
                final String codeStr = obj.get("code").getAsString();
                final int startLine = obj.get("startLine").getAsInt();
                final int endLine = obj.get("endLine").getAsInt();
                final String message = obj.get("message").getAsString();

                try {
                    final HeuristicCode code = HeuristicCode.valueOf(codeStr);
                    if (!config.enabledCodes().contains(code)) {
                        continue;
                    }
                    if (startLine < 1) {
                        continue;
                    }
                    findings.add(new Finding(
                            code, sourceFile, startLine, endLine,
                            message, Severity.WARNING, Confidence.LOW,
                            TOOL, codeStr, Map.of("model", config.model())));
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.FINE, "Unknown heuristic code in response: " + codeStr);
                }
            }

            return findings;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse Claude response as JSON: " + e.getMessage());
            return List.of();
        }
    }

    private List<Path> collectSourceFiles(ProjectContext context) {
        final List<PathMatcher> excludeMatchers = config.excludePatterns().stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();

        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> {
                    try (Stream<Path> walk = Files.walk(root)) {
                        walk.filter(p -> p.toString().endsWith(".java"))
                                .filter(p -> !isExcluded(p, excludeMatchers))
                                .filter(this::meetsMinLines)
                                .forEach(files::add);
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to walk source root: " + root, e);
                    }
                });
        return files;
    }

    private boolean isExcluded(Path file, List<PathMatcher> matchers) {
        return matchers.stream().anyMatch(m -> m.matches(file));
    }

    private boolean meetsMinLines(Path file) {
        try {
            return Files.lines(file).count() >= config.minFileLines();
        } catch (IOException e) {
            return false;
        }
    }

    private static String addLineNumbers(String content) {
        final String[] lines = content.split("\n", -1);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append(": ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    private static String extractJsonArray(String text) {
        final int start = text.indexOf('[');
        final int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "[]";
    }

    private String loadSystemPrompt() {
        try (InputStream is = getClass().getResourceAsStream("/claude-review-system.txt")) {
            if (is == null) {
                throw new IllegalStateException("System prompt resource not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system prompt", e);
        }
    }

    private static List<Finding> toCoreFindings(List<ReviewCache.CachedFinding> cached, String sourceFile) {
        return cached.stream()
                .map(cf -> new Finding(
                        HeuristicCode.valueOf(cf.code()), sourceFile,
                        cf.startLine(), cf.endLine(), cf.message(),
                        Severity.WARNING, Confidence.LOW, TOOL, cf.code(), Map.of()))
                .toList();
    }

    private static List<ReviewCache.CachedFinding> toCachedFindings(List<Finding> findings) {
        return findings.stream()
                .map(f -> new ReviewCache.CachedFinding(
                        f.code().name(), f.sourceFile(), f.startLine(), f.endLine(), f.message()))
                .toList();
    }
}
