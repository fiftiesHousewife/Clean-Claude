package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.RecipeThreshold;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.EnumMap;
import java.util.Map;

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

    @Inject
    public ThresholdsExtension() {
        propertiesByThreshold().forEach(
                (threshold, property) -> property.convention(threshold.defaultValue()));
    }

    Map<RecipeThreshold, Property<Integer>> propertiesByThreshold() {
        final EnumMap<RecipeThreshold, Property<Integer>> properties =
                new EnumMap<>(RecipeThreshold.class);
        properties.put(RecipeThreshold.CLASS_LINE_COUNT, getClassLineCount());
        properties.put(RecipeThreshold.RECORD_COMPONENT_COUNT, getRecordComponentCount());
        properties.put(RecipeThreshold.NULL_CHECK_DENSITY, getNullCheckDensity());
        properties.put(RecipeThreshold.CHAIN_DEPTH_THRESHOLD, getChainDepthThreshold());
        properties.put(RecipeThreshold.VERTICAL_SEPARATION_DISTANCE, getVerticalSeparationDistance());
        properties.put(RecipeThreshold.METHOD_BLANK_LINE_SECTIONS, getMethodBlankLineSections());
        properties.put(RecipeThreshold.PRIVATE_METHOD_MIN_LINES, getPrivateMethodMinLines());
        properties.put(RecipeThreshold.MAGIC_STRING_MIN_OCCURRENCES, getMagicStringMinOccurrences());
        properties.put(RecipeThreshold.STRING_SWITCH_MIN_CASES, getStringSwitchMinCases());
        properties.put(RecipeThreshold.SHORT_NAME_MIN_LENGTH, getShortNameMinLength());
        properties.put(RecipeThreshold.CPD_MINIMUM_TOKENS, getCpdMinimumTokens());
        properties.put(RecipeThreshold.MAGIC_NUMBER_MIN_VALUE, getMagicNumberMinValue());
        properties.put(RecipeThreshold.SECTION_COMMENT_THRESHOLD, getSectionCommentThreshold());
        properties.put(RecipeThreshold.HARDCODED_LIST_MIN_LITERALS, getHardcodedListMinLiterals());
        properties.put(RecipeThreshold.TEMPORAL_COUPLING_MIN_CALLS, getTemporalCouplingMinCalls());
        return properties;
    }
}
