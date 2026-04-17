package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.List;
import java.util.Map;

final class FindingCacheMapper {

    private final String tool;

    FindingCacheMapper(String tool) {
        this.tool = tool;
    }

    List<Finding> toFindings(List<ReviewCache.CachedFinding> cached, String sourceFile) {
        return cached.stream()
                .map(cf -> new Finding(
                        HeuristicCode.valueOf(cf.code()), sourceFile,
                        cf.startLine(), cf.endLine(), cf.message(),
                        Severity.WARNING, Confidence.LOW, tool, cf.code(), Map.of()))
                .toList();
    }

    List<ReviewCache.CachedFinding> toCachedFindings(List<Finding> findings) {
        return findings.stream()
                .map(f -> new ReviewCache.CachedFinding(
                        f.code().name(), f.sourceFile(), f.startLine(), f.endLine(), f.message()))
                .toList();
    }
}
