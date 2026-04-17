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

    public ClaudeReviewFindingSource(ClaudeReviewConfig config) {
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
    public boolean isAvailable(ProjectContext context) {
        return config.enabled() && config.hasApiKey();
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        if (!config.hasApiKey()) {
            return List.of();
        }
        final ReviewSession session = openSession(context);
        final List<Finding> allFindings = analyseAll(session);
        persistCache(session.cache());
        return allFindings;
    }

    List<Finding> parseFindings(String json, String sourceFile) {
        return parser.parse(json, sourceFile);
    }

    private ReviewSession openSession(ProjectContext context) {
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

    private List<Finding> analyseAll(ReviewSession session) {
        final List<Path> files = fileCollector.collect(session.context());
        final List<Finding> allFindings = new ArrayList<>();
        int analysed = 0;
        for (final Path file : files) {
            if (analysed >= config.maxFilesPerRun()) {
                break;
            }
            if (analyseOne(file, session, allFindings)) {
                analysed++;
            }
        }
        return allFindings;
    }

    private boolean analyseOne(Path file, ReviewSession session, List<Finding> sink) {
        try {
            final String content = Files.readString(file, StandardCharsets.UTF_8);
            final String relativePath = session.context().projectRoot().relativize(file).toString();
            final String hash = ReviewCache.hash(content, session.codesKey());
            final var cached = session.cache().lookup(hash);
            if (cached.isPresent()) {
                sink.addAll(cacheMapper.toFindings(cached.get(), relativePath));
                return false;
            }
            final List<Finding> findings = session.analyser().analyse(
                    new AnalysisRequest(content, relativePath, session.systemPrompt(), session.codesKey()));
            session.cache().store(hash, cacheMapper.toCachedFindings(findings));
            sink.addAll(findings);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read file: " + file, e);
            return false;
        }
    }

    private static void persistCache(ReviewCache cache) {
        try {
            cache.save();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save review cache", e);
        }
    }
}
