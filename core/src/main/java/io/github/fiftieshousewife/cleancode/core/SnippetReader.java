package io.github.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Reads a small window of source code around a finding so the HTML
 * report can show inline context without sending the user out to their
 * IDE. Quietly returns empty for findings that have no usable source
 * location (project-level findings, missing files, deleted lines).
 *
 * <p>Capped at {@link #MAX_LINES} total to avoid bloating the
 * single-file report; if the requested window exceeds the cap the
 * snippet is truncated symmetrically with the focal line(s) preserved.
 */
public final class SnippetReader {

    private static final int CONTEXT_BEFORE = 2;
    private static final int CONTEXT_AFTER = 2;
    static final int MAX_LINES = 12;

    public record Snippet(List<String> lines, int firstLineNumber, int focalStartLine, int focalEndLine) {
        public Snippet {
            lines = List.copyOf(lines);
        }
    }

    private SnippetReader() {}

    public static Optional<Snippet> read(final Finding finding, final Path projectRoot) {
        if (finding.sourceFile() == null || finding.startLine() <= 0 || projectRoot == null) {
            return Optional.empty();
        }
        final Path file = projectRoot.resolve(finding.sourceFile());
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        final List<String> all;
        try {
            all = Files.readAllLines(file);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (all.isEmpty()) {
            return Optional.empty();
        }

        final int totalLines = all.size();
        final int startLine = clamp(finding.startLine(), 1, totalLines);
        final int endLine = clamp(Math.max(finding.endLine(), finding.startLine()), startLine, totalLines);

        int from = Math.max(1, startLine - CONTEXT_BEFORE);
        int to = Math.min(totalLines, endLine + CONTEXT_AFTER);

        if (to - from + 1 > MAX_LINES) {
            final int focalSpan = endLine - startLine + 1;
            final int slack = Math.max(0, MAX_LINES - focalSpan);
            final int half = slack / 2;
            from = Math.max(1, startLine - half);
            to = Math.min(totalLines, from + MAX_LINES - 1);
        }

        return Optional.of(new Snippet(all.subList(from - 1, to), from, startLine, endLine));
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
