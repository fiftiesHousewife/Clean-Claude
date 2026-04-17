package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Set;

public record ClaudeReviewConfig(
        boolean enabled,
        String apiKey,
        String model,
        int maxFilesPerRun,
        int minFileLines,
        Set<HeuristicCode> enabledCodes,
        List<String> excludePatterns
) {
    public ClaudeReviewConfig {
        enabledCodes = Set.copyOf(enabledCodes);
        excludePatterns = List.copyOf(excludePatterns);
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
