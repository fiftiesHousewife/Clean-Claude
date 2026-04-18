package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.Finding;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters an {@link AggregatedReport} down to the findings that land on a
 * single file and turns each one into a {@link Suggestion}. The v1
 * detector deliberately stops at the finding level — it does not try to
 * propose concrete recipe tuples (method ranges, target FQNs, etc.); that
 * is left to the agent in {@link ReworkMode#AGENT_DRIVEN}. A future
 * {@code RECIPE_DIRECT} mode will need a richer detector that can
 * synthesise tuples on its own.
 */
public final class SuggestionDetector {

    private SuggestionDetector() {}

    public static List<Suggestion> suggestionsFor(final AggregatedReport report, final String filePath) {
        final List<Suggestion> suggestions = new ArrayList<>();
        report.findings().forEach(f -> {
            if (matchesFile(f, filePath)) {
                suggestions.add(new Suggestion(f.code(), f.startLine(), f.message()));
            }
        });
        return suggestions;
    }

    private static boolean matchesFile(final Finding f, final String filePath) {
        final String source = f.sourceFile();
        if (source == null) {
            return false;
        }
        return source.equals(filePath)
                || source.endsWith("/" + filePath)
                || source.endsWith(filePath);
    }
}
