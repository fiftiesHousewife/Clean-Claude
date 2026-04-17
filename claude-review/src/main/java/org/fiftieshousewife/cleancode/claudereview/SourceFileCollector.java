package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.core.ProjectContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

final class SourceFileCollector {

    private final List<PathMatcher> excludeMatchers;
    private final int minFileLines;

    SourceFileCollector(final ClaudeReviewConfig config) {
        final ClaudeReviewConfig.FileSelection fileSelection = config.fileSelection();
        this.excludeMatchers = fileSelection.excludePatterns().stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();
        this.minFileLines = fileSelection.minFileLines();
    }

    List<Path> collect(final ProjectContext context) {
        return context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .flatMap(this::walk)
                .toList();
    }

    private Stream<Path> walk(final Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !isExcluded(p))
                    .filter(this::meetsMinLines)
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk source root: " + root, e);
        }
    }

    private boolean isExcluded(final Path file) {
        return excludeMatchers.stream().anyMatch(m -> m.matches(file));
    }

    private boolean meetsMinLines(final Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.count() >= minFileLines;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to count lines in: " + file, e);
        }
    }
}
