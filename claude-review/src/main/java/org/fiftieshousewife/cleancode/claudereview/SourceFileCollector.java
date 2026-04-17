package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.core.ProjectContext;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class SourceFileCollector {

    private static final Logger LOG = Logger.getLogger(SourceFileCollector.class.getName());

    private final List<PathMatcher> excludeMatchers;
    private final int minFileLines;

    SourceFileCollector(final ClaudeReviewConfig config) {
        this.excludeMatchers = config.excludePatterns().stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();
        this.minFileLines = config.minFileLines();
    }

    List<Path> collect(final ProjectContext context) {
        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> walk(root, files));
        return files;
    }

    private void walk(final Path root, final List<Path> files) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !isExcluded(p))
                    .filter(this::meetsMinLines)
                    .forEach(files::add);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to walk source root: " + root, e);
        }
    }

    private boolean isExcluded(final Path file) {
        return excludeMatchers.stream().anyMatch(m -> m.matches(file));
    }

    private boolean meetsMinLines(final Path file) {
        try {
            return Files.lines(file).count() >= minFileLines;
        } catch (IOException e) {
            return false;
        }
    }
}
