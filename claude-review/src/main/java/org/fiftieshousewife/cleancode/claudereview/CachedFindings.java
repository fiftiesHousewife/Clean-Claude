package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.claudereview.ReviewCache.CachedFinding;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.List;
import java.util.Map;

final class CachedFindings {

    private static final String TOOL = "claude-review";

    private CachedFindings() {
    }

    static List<Finding> toFindings(final List<CachedFinding> cached, final String sourceFile) {
        return cached.stream()
                .map(cf -> new Finding(
                        HeuristicCode.valueOf(cf.code()), sourceFile,
                        cf.startLine(), cf.endLine(), cf.message(),
                        Severity.WARNING, Confidence.LOW, TOOL, cf.code(), Map.of()))
                .toList();
    }

    static List<CachedFinding> fromFindings(final List<Finding> findings) {
        return findings.stream()
                .map(f -> new CachedFinding(
                        f.code().name(), f.sourceFile(), f.startLine(), f.endLine(), f.message()))
                .toList();
    }
}
