package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The return value from a {@link ReworkOrchestrator} run. Carries the
 * list of files the session touched (often one, but up to however many
 * the task handed in), the suggestions the plugin proposed, the actions
 * and rejections the agent reported back (empty in
 * {@link ReworkMode#SUGGEST_ONLY}), the token accounting when the agent
 * runtime supplied it, and a pre-formatted commit-message body built
 * from those fields.
 */
public record ReworkReport(
        List<Path> files,
        ReworkMode mode,
        List<Suggestion> suggestions,
        List<AgentAction> actionsTaken,
        List<AgentRejection> rejected,
        Optional<AgentUsage> usage,
        String commitMessageBody) {}
