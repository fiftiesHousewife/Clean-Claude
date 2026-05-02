package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.util.List;
import java.util.Map;

/**
 * Read-only view of the current {@code cleanCode} configuration that the
 * HTML report needs in order to render correct toggle states (e.g. "this
 * code is already disabled, show the toggle as off").
 */
public record ConfigSnapshot(
        List<String> disabledRecipes,
        List<String> enabledOptionalRules,
        Map<String, Integer> thresholds) {

    public ConfigSnapshot {
        disabledRecipes = List.copyOf(disabledRecipes);
        enabledOptionalRules = List.copyOf(enabledOptionalRules);
        thresholds = Map.copyOf(thresholds);
    }
}
