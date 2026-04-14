package org.fiftieshousewife.cleancode.core;

public record RecipeThresholds(
        int classLineCount,
        int recordComponentCount,
        int nullCheckDensity,
        int chainDepthThreshold,
        int verticalSeparationDistance,
        int methodBlankLineSections,
        int privateMethodMinLines,
        int magicStringMinOccurrences,
        int stringSwitchMinCases,
        int shortNameMinLength
) {

    public static final int DEFAULT_CLASS_LINE_COUNT = 150;
    public static final int DEFAULT_RECORD_COMPONENT_COUNT = 4;
    public static final int DEFAULT_NULL_CHECK_DENSITY = 3;
    public static final int DEFAULT_CHAIN_DEPTH_THRESHOLD = 3;
    public static final int DEFAULT_VERTICAL_SEPARATION_DISTANCE = 5;
    public static final int DEFAULT_METHOD_BLANK_LINE_SECTIONS = 2;
    public static final int DEFAULT_PRIVATE_METHOD_MIN_LINES = 5;
    public static final int DEFAULT_MAGIC_STRING_MIN_OCCURRENCES = 2;
    public static final int DEFAULT_STRING_SWITCH_MIN_CASES = 3;
    public static final int DEFAULT_SHORT_NAME_MIN_LENGTH = 2;

    public static RecipeThresholds defaults() {
        return new RecipeThresholds(
                DEFAULT_CLASS_LINE_COUNT,
                DEFAULT_RECORD_COMPONENT_COUNT,
                DEFAULT_NULL_CHECK_DENSITY,
                DEFAULT_CHAIN_DEPTH_THRESHOLD,
                DEFAULT_VERTICAL_SEPARATION_DISTANCE,
                DEFAULT_METHOD_BLANK_LINE_SECTIONS,
                DEFAULT_PRIVATE_METHOD_MIN_LINES,
                DEFAULT_MAGIC_STRING_MIN_OCCURRENCES,
                DEFAULT_STRING_SWITCH_MIN_CASES,
                DEFAULT_SHORT_NAME_MIN_LENGTH);
    }
}
