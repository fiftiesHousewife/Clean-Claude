package org.fiftieshousewife.cleancode.plugin.rework;

import java.nio.file.Path;
import java.util.List;

/**
 * The return value from a {@link ReworkOrchestrator} run. Carries the
 * suggestions the plugin proposed, the actions and rejections the agent
 * reported back (empty in {@link ReworkMode#SUGGEST_ONLY}), and a
 * pre-formatted commit-message body built from those fields.
 */
public record ReworkReport(
        Path file,
        ReworkMode mode,
        List<Suggestion> suggestions,
        List<AgentAction> actionsTaken,
        List<AgentRejection> rejected,
        String commitMessageBody) {}
