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

    @SuppressWarnings("this-escape")
    public abstract static class ThresholdsExtension {

        public abstract Property<Integer> getClassLineCount();

        public abstract Property<Integer> getRecordComponentCount();

        public abstract Property<Integer> getNullCheckDensity();

        public abstract Property<Integer> getChainDepthThreshold();

        public abstract Property<Integer> getVerticalSeparationDistance();

        public abstract Property<Integer> getMethodBlankLineSections();

        public abstract Property<Integer> getPrivateMethodMinLines();

        public abstract Property<Integer> getMagicStringMinOccurrences();

        public abstract Property<Integer> getStringSwitchMinCases();

        public abstract Property<Integer> getShortNameMinLength();

        public abstract Property<Integer> getCpdMinimumTokens();

        public abstract Property<Integer> getMagicNumberMinValue();

        public abstract Property<Integer> getSectionCommentThreshold();

        public abstract Property<Integer> getHardcodedListMinLiterals();

        public abstract Property<Integer> getTemporalCouplingMinCalls();

        public abstract Property<Integer> getLineLengthErrorThreshold();

        @Inject
        public ThresholdsExtension() {
            getClassLineCount().convention(RecipeThresholds.DEFAULT_CLASS_LINE_COUNT);
            getRecordComponentCount().convention(RecipeThresholds.DEFAULT_RECORD_COMPONENT_COUNT);
            getNullCheckDensity().convention(RecipeThresholds.DEFAULT_NULL_CHECK_DENSITY);
            getChainDepthThreshold().convention(RecipeThresholds.DEFAULT_CHAIN_DEPTH_THRESHOLD);
            getVerticalSeparationDistance().convention(RecipeThresholds.DEFAULT_VERTICAL_SEPARATION_DISTANCE);
            getMethodBlankLineSections().convention(RecipeThresholds.DEFAULT_METHOD_BLANK_LINE_SECTIONS);
            getPrivateMethodMinLines().convention(RecipeThresholds.DEFAULT_PRIVATE_METHOD_MIN_LINES);
            getMagicStringMinOccurrences().convention(RecipeThresholds.DEFAULT_MAGIC_STRING_MIN_OCCURRENCES);
            getStringSwitchMinCases().convention(RecipeThresholds.DEFAULT_STRING_SWITCH_MIN_CASES);
            getShortNameMinLength().convention(RecipeThresholds.DEFAULT_SHORT_NAME_MIN_LENGTH);
            getCpdMinimumTokens().convention(RecipeThresholds.DEFAULT_CPD_MINIMUM_TOKENS);
            getMagicNumberMinValue().convention(RecipeThresholds.DEFAULT_MAGIC_NUMBER_MIN_VALUE);
            getSectionCommentThreshold().convention(RecipeThresholds.DEFAULT_SECTION_COMMENT_THRESHOLD);
            getHardcodedListMinLiterals().convention(RecipeThresholds.DEFAULT_HARDCODED_LIST_MIN_LITERALS);
            getTemporalCouplingMinCalls().convention(RecipeThresholds.DEFAULT_TEMPORAL_COUPLING_MIN_CALLS);
            getLineLengthErrorThreshold().convention(RecipeThresholds.DEFAULT_LINE_LENGTH_ERROR_THRESHOLD);
        }
    }

    @SuppressWarnings("this-escape")
    public abstract static class ClaudeReviewExtension {

        public abstract Property<Boolean> getEnabled();

        public abstract Property<String> getModel();

        public abstract Property<Integer> getMaxFilesPerRun();

        public abstract Property<Integer> getMinFileLines();

        public abstract ListProperty<String> getCodes();

        public abstract ListProperty<String> getExcludePatterns();

        @Inject
        public ClaudeReviewExtension() {
            getEnabled().convention(false);
            getModel().convention("claude-sonnet-4-6");
            getMaxFilesPerRun().convention(50);
            getMinFileLines().convention(10);
            getCodes().convention(java.util.List.of("G6", "G20", "N4"));
            getExcludePatterns().convention(java.util.List.of("**/generated/**"));
        }
    }
}
