package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Set;

public record ClaudeReviewConfig(
        boolean enabled,
        String apiKey,
        String model,
        Set<HeuristicCode> enabledCodes,
        FileSelection fileSelection
) {
    public ClaudeReviewConfig {
        enabledCodes = Set.copyOf(enabledCodes);
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public record FileSelection(
            int maxFilesPerRun,
            int minFileLines,
            List<String> excludePatterns
    ) {
        public FileSelection {
            excludePatterns = List.copyOf(excludePatterns);
        }
    }
}
