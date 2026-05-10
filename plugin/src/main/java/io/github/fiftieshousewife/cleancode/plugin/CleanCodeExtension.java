package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@SuppressWarnings("this-escape")
public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    public abstract ListProperty<String> getDisabledRecipes();

    public abstract ListProperty<String> getEnabledOptionalRules();

    public abstract MapProperty<String, java.util.List<String>> getPackageSuppressions();

    public abstract Property<String> getSkillsDir();

    public abstract Property<String> getAgentInstructionsFile();

    public abstract Property<String> getRepositoryUrl();

    public abstract Property<String> getIdeUrlScheme();

    public abstract Property<Boolean> getEnforceFormatting();

    public abstract Property<Integer> getServePort();

    private final ThresholdsExtension thresholds;

    @Inject
    public CleanCodeExtension(final ObjectFactory objects) {
        getFailOnViolation().convention(true);
        getDisabledRecipes().convention(java.util.List.of());
        getEnabledOptionalRules().convention(java.util.List.of());
        getPackageSuppressions().convention(java.util.Map.of());
        getSkillsDir().convention("");
        getAgentInstructionsFile().convention("");
        getRepositoryUrl().convention("");
        getIdeUrlScheme().convention("");
        getEnforceFormatting().convention(false);
        getServePort().convention(7070);
        thresholds = objects.newInstance(ThresholdsExtension.class);
    }

    public void thresholds(final Action<? super ThresholdsExtension> action) {
        action.execute(thresholds);
    }

    public ThresholdsExtension getThresholds() {
        return thresholds;
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

}
