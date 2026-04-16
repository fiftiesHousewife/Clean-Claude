package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("this-escape")
public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    public abstract ListProperty<String> getDisabledRecipes();

    public abstract MapProperty<String, java.util.List<String>> getPackageSuppressions();

    public abstract Property<String> getSkillsDir();

    public abstract Property<String> getRepositoryUrl();

    private final ThresholdsExtension thresholds;
    private final ClaudeReviewExtension claudeReview;

    @Inject
    public CleanCodeExtension(ObjectFactory objects) {
        getFailOnViolation().convention(true);
        getDisabledRecipes().convention(java.util.List.of());
        getPackageSuppressions().convention(java.util.Map.of());
        getSkillsDir().convention(".claude/skills");
        getRepositoryUrl().convention("");
        thresholds = objects.newInstance(ThresholdsExtension.class);
        claudeReview = objects.newInstance(ClaudeReviewExtension.class);
    }

    public void thresholds(Action<? super ThresholdsExtension> action) {
        action.execute(thresholds);
    }

    public ThresholdsExtension getThresholds() {
        return thresholds;
    }

    public void claudeReview(Action<? super ClaudeReviewExtension> action) {
        action.execute(claudeReview);
    }

    public ClaudeReviewExtension getClaudeReview() {
        return claudeReview;
    }

    public ClaudeReviewConfig buildClaudeReviewConfig(String apiKey) {
        final Set<HeuristicCode> enabledCodes = claudeReview.getCodes().get().stream()
                .map(HeuristicCode::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        return new ClaudeReviewConfig(
                claudeReview.getEnabled().get(),
                apiKey,
                claudeReview.getModel().get(),
                claudeReview.getMaxFilesPerRun().get(),
                claudeReview.getMinFileLines().get(),
                enabledCodes,
                claudeReview.getExcludePatterns().get());
    }

    public RecipeThresholds buildRecipeThresholds() {
        final java.util.Map<org.fiftieshousewife.cleancode.core.RecipeThreshold, Integer> values =
                new java.util.EnumMap<>(org.fiftieshousewife.cleancode.core.RecipeThreshold.class);
        thresholds.propertiesByThreshold().forEach(
                (threshold, property) -> values.put(threshold, property.get()));
        return RecipeThresholds.of(values);
    }
}
