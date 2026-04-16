package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.claudereview.ClaudeReviewConfig;
import org.fiftieshousewife.cleancode.core.RecipeThreshold;
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
        final java.util.Map<RecipeThreshold, Integer> values = new java.util.EnumMap<>(RecipeThreshold.class);
        values.put(RecipeThreshold.CLASS_LINE_COUNT, thresholds.getClassLineCount().get());
        values.put(RecipeThreshold.RECORD_COMPONENT_COUNT, thresholds.getRecordComponentCount().get());
        values.put(RecipeThreshold.NULL_CHECK_DENSITY, thresholds.getNullCheckDensity().get());
        values.put(RecipeThreshold.CHAIN_DEPTH_THRESHOLD, thresholds.getChainDepthThreshold().get());
        values.put(RecipeThreshold.VERTICAL_SEPARATION_DISTANCE, thresholds.getVerticalSeparationDistance().get());
        values.put(RecipeThreshold.METHOD_BLANK_LINE_SECTIONS, thresholds.getMethodBlankLineSections().get());
        values.put(RecipeThreshold.PRIVATE_METHOD_MIN_LINES, thresholds.getPrivateMethodMinLines().get());
        values.put(RecipeThreshold.MAGIC_STRING_MIN_OCCURRENCES, thresholds.getMagicStringMinOccurrences().get());
        values.put(RecipeThreshold.STRING_SWITCH_MIN_CASES, thresholds.getStringSwitchMinCases().get());
        values.put(RecipeThreshold.SHORT_NAME_MIN_LENGTH, thresholds.getShortNameMinLength().get());
        values.put(RecipeThreshold.CPD_MINIMUM_TOKENS, thresholds.getCpdMinimumTokens().get());
        values.put(RecipeThreshold.MAGIC_NUMBER_MIN_VALUE, thresholds.getMagicNumberMinValue().get());
        values.put(RecipeThreshold.SECTION_COMMENT_THRESHOLD, thresholds.getSectionCommentThreshold().get());
        values.put(RecipeThreshold.HARDCODED_LIST_MIN_LITERALS, thresholds.getHardcodedListMinLiterals().get());
        values.put(RecipeThreshold.TEMPORAL_COUPLING_MIN_CALLS, thresholds.getTemporalCouplingMinCalls().get());
        return RecipeThresholds.of(values);
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

        @Inject
        public ThresholdsExtension() {
            getClassLineCount().convention(RecipeThreshold.CLASS_LINE_COUNT.defaultValue());
            getRecordComponentCount().convention(RecipeThreshold.RECORD_COMPONENT_COUNT.defaultValue());
            getNullCheckDensity().convention(RecipeThreshold.NULL_CHECK_DENSITY.defaultValue());
            getChainDepthThreshold().convention(RecipeThreshold.CHAIN_DEPTH_THRESHOLD.defaultValue());
            getVerticalSeparationDistance().convention(RecipeThreshold.VERTICAL_SEPARATION_DISTANCE.defaultValue());
            getMethodBlankLineSections().convention(RecipeThreshold.METHOD_BLANK_LINE_SECTIONS.defaultValue());
            getPrivateMethodMinLines().convention(RecipeThreshold.PRIVATE_METHOD_MIN_LINES.defaultValue());
            getMagicStringMinOccurrences().convention(RecipeThreshold.MAGIC_STRING_MIN_OCCURRENCES.defaultValue());
            getStringSwitchMinCases().convention(RecipeThreshold.STRING_SWITCH_MIN_CASES.defaultValue());
            getShortNameMinLength().convention(RecipeThreshold.SHORT_NAME_MIN_LENGTH.defaultValue());
            getCpdMinimumTokens().convention(RecipeThreshold.CPD_MINIMUM_TOKENS.defaultValue());
            getMagicNumberMinValue().convention(RecipeThreshold.MAGIC_NUMBER_MIN_VALUE.defaultValue());
            getSectionCommentThreshold().convention(RecipeThreshold.SECTION_COMMENT_THRESHOLD.defaultValue());
            getHardcodedListMinLiterals().convention(RecipeThreshold.HARDCODED_LIST_MIN_LITERALS.defaultValue());
            getTemporalCouplingMinCalls().convention(RecipeThreshold.TEMPORAL_COUPLING_MIN_CALLS.defaultValue());
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
