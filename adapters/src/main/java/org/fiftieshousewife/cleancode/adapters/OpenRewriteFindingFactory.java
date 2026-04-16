package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.Map;

final class OpenRewriteFindingFactory {

    static final String TOOL = "openrewrite";

    private final Map<String, String> classNameToSourcePath;

    OpenRewriteFindingFactory(final Map<String, String> classNameToSourcePath) {
        this.classNameToSourcePath = classNameToSourcePath;
    }

    Finding finding(final HeuristicCode code, final String className, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, -1, -1,
                message, Severity.WARNING, Confidence.HIGH, TOOL, code.name());
    }

    Finding finding(final HeuristicCode code, final String className, final int line, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, line, line,
                message, Severity.WARNING, Confidence.HIGH, TOOL, code.name());
    }

    String resolveSourcePath(final String className) {
        return classNameToSourcePath.getOrDefault(className, className + ".java");
    }
}
