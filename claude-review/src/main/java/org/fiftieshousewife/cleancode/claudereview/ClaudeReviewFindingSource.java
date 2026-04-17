package org.fiftieshousewife.cleancode.claudereview;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClaudeReviewFindingSource implements FindingSource {

    private static final String TOOL = "claude-review";
    private static final Logger LOG = Logger.getLogger(ClaudeReviewFindingSource.class.getName());

    private final ClaudeReviewConfig config;
    private final ClaudeFindingParser parser;
    private final FindingCacheMapper cacheMapper;
    private final SourceFileCollector fileCollector;
    private final SystemPromptLoader promptLoader;

    public ClaudeReviewFindingSource(final ClaudeReviewConfig config) {
        this.config = config;
        this.parser = new ClaudeFindingParser(config.enabledCodes(), TOOL, config.model());
        this.cacheMapper = new FindingCacheMapper(TOOL);
        this.fileCollector = new SourceFileCollector(config.excludePatterns(), config.minFileLines());
        this.promptLoader = new SystemPromptLoader();
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
        return config.enabledCodes();
    }

    @Override
    public boolean isAvailable(final ProjectContext context) {
        return config.enabled() && config.hasApiKey();
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        if (!config.hasApiKey()) {
            return List.of();
        }
        final ReviewSession session = openSession(context);
        final List<Finding> allFindings = analyseAll(session);
        persistCache(session.cache());
        return allFindings;
    }

    List<Finding> parseFindings(final String json, final String sourceFile) {
        return parser.parse(json, sourceFile);
    }

    ReviewSession openSession(final ProjectContext context) {
        final AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(config.apiKey()).build();
        final ClaudeFileAnalyser analyser = new ClaudeFileAnalyser(client, config.model(), parser);
        final String systemPrompt = promptLoader.load();
        final String codesKey = buildCodesKey();
        final ReviewCache cache = ReviewCache.load(context.buildDir().resolve("claude-review-cache"));
        return new ReviewSession(context, analyser, systemPrompt, codesKey, cache);
    }

    private String buildCodesKey() {
        return config.enabledCodes().stream()
                .map(HeuristicCode::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private List<Finding> analyseAll(final ReviewSession session) {
        final List<Path> files = fileCollector.collect(session.context());
        final List<Finding> allFindings = new ArrayList<>();
        int analysed = 0;
        for (final Path file : files) {
            if (analysed >= config.maxFilesPerRun()) {
                break;
            }
            final Optional<FileAnalysisResult> result = analyseOne(file, session);
            if (result.isEmpty()) {
                continue;
            }
            allFindings.addAll(result.get().findings());
            if (result.get().freshlyAnalysed()) {
                analysed++;
            }
        }
        return allFindings;
    }

    private Optional<FileAnalysisResult> analyseOne(final Path file, final ReviewSession session) {
        try {
            final String content = Files.readString(file, StandardCharsets.UTF_8);
            final String relativePath = session.context().projectRoot().relativize(file).toString();
            final String hash = ReviewCache.hash(content, session.codesKey());
            final var cached = session.cache().lookup(hash);
            if (cached.isPresent()) {
                return Optional.of(new FileAnalysisResult(cacheMapper.toFindings(cached.get(), relativePath), false));
            }
            final List<Finding> findings = session.analyser().analyse(
                    new AnalysisRequest(content, relativePath, session.systemPrompt(), session.codesKey()));
            session.cache().store(hash, cacheMapper.toCachedFindings(findings));
            return Optional.of(new FileAnalysisResult(findings, true));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read file: " + file, e);
            return Optional.empty();
        }
    }

    private static void persistCache(final ReviewCache cache) throws FindingSourceException {
        try {
            cache.save();
        } catch (IOException e) {
            throw new FindingSourceException("Failed to save review cache", e);
        }
    }

    private record FileAnalysisResult(List<Finding> findings, boolean freshlyAnalysed) {}
}
