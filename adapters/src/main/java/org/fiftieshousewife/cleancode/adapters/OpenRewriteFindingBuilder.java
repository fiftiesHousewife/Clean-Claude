package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.openrewrite.SourceFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class OpenRewriteFindingBuilder {

    static final String TOOL = "openrewrite";

    private final Map<String, String> classNameToSourcePath;

    private OpenRewriteFindingBuilder(final Map<String, String> classNameToSourcePath) {
        this.classNameToSourcePath = classNameToSourcePath;
    }

    static OpenRewriteFindingBuilder forSourceFiles(final List<SourceFile> parsed) {
        return new OpenRewriteFindingBuilder(buildSourcePathIndex(parsed));
    }

    Finding finding(final HeuristicCode code, final String className, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, -1, -1,
                message, OpenRewriteFindingSource.severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    Finding finding(final HeuristicCode code, final String className,
                    final int line, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, line, line,
                message, OpenRewriteFindingSource.severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    <R> List<Finding> mapRows(final List<R> rows, final Function<R, Finding> rowToFinding) {
        return rows.stream().map(rowToFinding).toList();
    }

    private String resolveSourcePath(final String className) {
        return classNameToSourcePath.getOrDefault(className, className + ".java");
    }

    private static Map<String, String> buildSourcePathIndex(final List<SourceFile> parsed) {
        final Map<String, String> index = new HashMap<>();
        parsed.forEach(sf -> {
            final String path = sf.getSourcePath().toString();
            final String fileName = path.contains("/")
                    ? path.substring(path.lastIndexOf('/') + 1)
                    : path;
            final String className = fileName.endsWith(".java")
                    ? fileName.substring(0, fileName.length() - 5)
                    : fileName;
            index.put(className, path);
        });
        return index;
    }
}
