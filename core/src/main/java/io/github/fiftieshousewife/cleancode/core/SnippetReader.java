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

        // The default symmetric window pulls Javadoc / annotations from above
        // the focal line into the snippet. For class- and method-level
        // findings (Ch10_1 file-length, EI_EXPOSE on records, Ch7_1 on a
        // method's catch block) that means the user sees the comment about
        // the method instead of a glimpse of its body. Detect noisy
        // context-before — Javadoc lines, block comments, line comments,
        // annotations, blanks — and trim them, spending the saved budget on
        // more lines AFTER the focal instead.
        final int contextBefore = trimmedBefore(all, startLine);
        final int contextAfter = CONTEXT_AFTER + (CONTEXT_BEFORE - contextBefore);

        int from = Math.max(1, startLine - contextBefore);
        int to = Math.min(totalLines, endLine + contextAfter);

        if (to - from + 1 > MAX_LINES) {
            final int focalSpan = endLine - startLine + 1;
            final int slack = Math.max(0, MAX_LINES - focalSpan);
            final int half = slack / 2;
            from = Math.max(1, startLine - half);
            to = Math.min(totalLines, from + MAX_LINES - 1);
        }

        return Optional.of(new Snippet(all.subList(from - 1, to), from, startLine, endLine));
    }

    /**
     * Returns how many lines of context-before to actually include — at most
     * {@link #CONTEXT_BEFORE}, less if any of those candidate lines is
     * Javadoc, a comment, an annotation, or blank. The first non-noise line
     * we encounter walking upwards from the focal sets the cap; everything
     * above it is also kept since it's "real" code, but the moment we see
     * a comment line we stop adding more before-context.
     */
    private static int trimmedBefore(final List<String> all, final int focalLine) {
        int kept = 0;
        for (int offset = 1; offset <= CONTEXT_BEFORE; offset++) {
            final int idx = focalLine - 1 - offset;
            if (idx < 0) {
                break;
            }
            if (isNoise(all.get(idx))) {
                break;
            }
            kept = offset;
        }
        return kept;
    }

    private static boolean isNoise(final String line) {
        final String stripped = line.strip();
        if (stripped.isEmpty()) {
            return true;
        }
        return stripped.startsWith("*")             // Javadoc body or closing */
                || stripped.startsWith("/*")        // block comment opening
                || stripped.startsWith("//")        // line comment
                || stripped.startsWith("@");        // annotation (e.g. @Override)
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
