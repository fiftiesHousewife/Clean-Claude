package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Set;

public record ClaudeReviewConfig(
        boolean enabled,
        String model,
        int maxFilesPerRun,
        int minFileLines,
        Set<HeuristicCode> enabledCodes,
        List<String> excludePatterns
) {}
