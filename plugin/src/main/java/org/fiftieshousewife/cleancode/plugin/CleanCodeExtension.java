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

public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    public abstract ListProperty<String> getDisabledRecipes();

    public abstract MapProperty<String, java.util.List<String>> getPackageSuppressions();

    public abstract Property<String> getSkillsDir();

    public abstract Property<String> getRepositoryUrl();

    public abstract Property<Boolean> getEnforceFormatting();

    private final ThresholdsExtension thresholds;
    private final ClaudeReviewExtension claudeReview;

    @Inject
    @SuppressWarnings("this-escape")
    public CleanCodeExtension(ObjectFactory objects) {
        getFailOnViolation().convention(true);
        getDisabledRecipes().convention(java.util.List.of());
        getPackageSuppressions().convention(java.util.Map.of());
        getSkillsDir().convention(".claude/skills");
        getRepositoryUrl().convention("");
        getEnforceFormatting().convention(false);
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
        final ClaudeReviewConfig.FileSelection fileSelection = new ClaudeReviewConfig.FileSelection(
                claudeReview.getMaxFilesPerRun().get(),
                claudeReview.getMinFileLines().get(),
                claudeReview.getExcludePatterns().get());
        return new ClaudeReviewConfig(
                claudeReview.getEnabled().get(),
                apiKey,
                claudeReview.getModel().get(),
                enabledCodes,
                fileSelection);
    }

    public RecipeThresholds buildRecipeThresholds() {
        return new RecipeThresholds(
                thresholds.getClassLineCount().get(),
                thresholds.getRecordComponentCount().get(),
                thresholds.getNullCheckDensity().get(),
                thresholds.getChainDepthThreshold().get(),
                thresholds.getVerticalSeparationDistance().get(),
                thresholds.getMethodBlankLineSections().get(),
                thresholds.getPrivateMethodMinLines().get(),
                thresholds.getMagicStringMinOccurrences().get(),
                thresholds.getStringSwitchMinCases().get(),
                thresholds.getShortNameMinLength().get(),
                thresholds.getCpdMinimumTokens().get(),
                thresholds.getMagicNumberMinValue().get(),
                thresholds.getSectionCommentThreshold().get(),
                thresholds.getHardcodedListMinLiterals().get(),
                thresholds.getTemporalCouplingMinCalls().get(),
                thresholds.getLineLengthErrorThreshold().get());
    }
}
