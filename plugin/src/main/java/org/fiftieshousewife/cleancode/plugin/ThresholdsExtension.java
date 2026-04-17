package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@SuppressWarnings("this-escape")
public abstract class ThresholdsExtension {

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
