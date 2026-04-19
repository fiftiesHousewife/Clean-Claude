package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

/**
 * A hint produced by {@link SuggestionDetector} from a single finding on
 * the target file. Suggestions are deliberately coarse — they name the
 * heuristic, the line, and a human-readable description — and leave the
 * concrete recipe-option tuple to the agent or to a future
 * {@code RECIPE_DIRECT} mode detector.
 */
public record Suggestion(HeuristicCode code, int line, String message) {}
