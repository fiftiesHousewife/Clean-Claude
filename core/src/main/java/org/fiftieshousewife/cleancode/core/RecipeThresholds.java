package org.fiftieshousewife.cleancode.core;

import java.util.EnumMap;
import java.util.Map;

public final class RecipeThresholds {

    private final Map<RecipeThreshold, Integer> values;

    private RecipeThresholds(final Map<RecipeThreshold, Integer> values) {
        this.values = new EnumMap<>(values);
    }

    public static RecipeThresholds defaults() {
        final Map<RecipeThreshold, Integer> values = new EnumMap<>(RecipeThreshold.class);
        for (final RecipeThreshold threshold : RecipeThreshold.values()) {
            values.put(threshold, threshold.defaultValue());
        }
        return new RecipeThresholds(values);
    }

    public static RecipeThresholds of(final Map<RecipeThreshold, Integer> values) {
        final Map<RecipeThreshold, Integer> resolved = new EnumMap<>(RecipeThreshold.class);
        for (final RecipeThreshold threshold : RecipeThreshold.values()) {
            resolved.put(threshold, values.getOrDefault(threshold, threshold.defaultValue()));
        }
        return new RecipeThresholds(resolved);
    }

    public int get(final RecipeThreshold threshold) {
        return values.get(threshold);
    }
}
