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

    SourceFileCollector(List<String> excludePatterns, int minFileLines) {
        this.excludeMatchers = excludePatterns.stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();
        this.minFileLines = minFileLines;
    }

    List<Path> collect(ProjectContext context) {
        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> walkRoot(root, files));
        return files;
    }

    private void walkRoot(Path root, List<Path> sink) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !isExcluded(p))
                    .filter(this::meetsMinLines)
                    .forEach(sink::add);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to walk source root: " + root, e);
        }
    }

    private boolean isExcluded(Path file) {
        return excludeMatchers.stream().anyMatch(m -> m.matches(file));
    }

    private boolean meetsMinLines(Path file) {
        try {
            return Files.lines(file).count() >= minFileLines;
        } catch (IOException e) {
            return false;
        }
    }
}
