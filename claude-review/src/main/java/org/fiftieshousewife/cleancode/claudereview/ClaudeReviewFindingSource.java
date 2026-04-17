package org.fiftieshousewife.cleancode.claudereview;

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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClaudeReviewFindingSource implements FindingSource {

    private static final String TOOL = "claude-review";
    private static final Logger LOG = Logger.getLogger(ClaudeReviewFindingSource.class.getName());

    private final ClaudeReviewConfig config;
    private final FindingJsonParser parser;

    public ClaudeReviewFindingSource(final ClaudeReviewConfig config) {
        this.config = config;
        this.parser = new FindingJsonParser(config);
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
    public boolean isAvailable(final ProjectContext context) {
        return config.enabled() && config.hasApiKey();
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        if (!config.hasApiKey()) {
            return List.of();
        }
        final ReviewSession session = newSession(context);
        final List<Path> files = new SourceFileCollector(config).collect(context);
        final List<Finding> allFindings = reviewFiles(files, session);
        persistCache(session);
        return allFindings;
    }

    List<Finding> parseFindings(final String json, final String sourceFile) {
        return parser.parse(json, sourceFile);
    }

    private ReviewSession newSession(final ProjectContext context) {
        final String codesKey = config.enabledCodes().stream()
                .map(HeuristicCode::name)
                .sorted()
                .collect(Collectors.joining(","));
        final Path cacheDir = context.buildDir().resolve("claude-review-cache");
        return new ReviewSession(context, new ClaudeReviewer(config, codesKey),
                ReviewCache.load(cacheDir), cacheDir, codesKey);
    }

    private List<Finding> reviewFiles(final List<Path> files, final ReviewSession session) {
        final List<Finding> allFindings = new ArrayList<>();
        int analysed = 0;
        for (final Path file : files) {
            if (analysed >= config.maxFilesPerRun()) {
                break;
            }
            if (reviewSingleFile(file, session, allFindings)) {
                analysed++;
            }
        }
        return allFindings;
    }

    private boolean reviewSingleFile(final Path file, final ReviewSession session,
                                     final List<Finding> allFindings) {
        try {
            final String content = Files.readString(file, StandardCharsets.UTF_8);
            final String relativePath = session.context().projectRoot().relativize(file).toString();
            final String hash = ReviewCache.hash(content, session.codesKey());
            final var cached = session.cache().lookup(hash);
            if (cached.isPresent()) {
                allFindings.addAll(CachedFindings.toFindings(cached.get(), relativePath));
                return false;
            }
            final List<Finding> findings = parser.parse(
                    session.reviewer().review(content, relativePath), relativePath);
            session.cache().store(hash, CachedFindings.fromFindings(findings));
            allFindings.addAll(findings);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read file: " + file, e);
            return false;
        }
    }

    private static void persistCache(final ReviewSession session) {
        try {
            session.cache().save(session.cacheDir());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save review cache", e);
        }
    }

    private record ReviewSession(
            ProjectContext context,
            ClaudeReviewer reviewer,
            ReviewCache cache,
            Path cacheDir,
            String codesKey) {
    }
}
