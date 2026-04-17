package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.core.ProjectContext;

record ReviewSession(
        ProjectContext context,
        ClaudeFileAnalyser analyser,
        String systemPrompt,
        String codesKey,
        ReviewCache cache
) {}
