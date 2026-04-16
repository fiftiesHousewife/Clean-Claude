package org.fiftieshousewife.cleancode.core;

public enum RecipeThreshold {

    CLASS_LINE_COUNT(150),
    RECORD_COMPONENT_COUNT(6),
    NULL_CHECK_DENSITY(3),
    CHAIN_DEPTH_THRESHOLD(3),
    VERTICAL_SEPARATION_DISTANCE(10),
    METHOD_BLANK_LINE_SECTIONS(4),
    PRIVATE_METHOD_MIN_LINES(5),
    MAGIC_STRING_MIN_OCCURRENCES(2),
    STRING_SWITCH_MIN_CASES(3),
    SHORT_NAME_MIN_LENGTH(2),
    CPD_MINIMUM_TOKENS(50),
    MAGIC_NUMBER_MIN_VALUE(1),
    SECTION_COMMENT_THRESHOLD(1),
    HARDCODED_LIST_MIN_LITERALS(5),
    TEMPORAL_COUPLING_MIN_CALLS(3);

    private final int defaultValue;

    RecipeThreshold(final int defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int defaultValue() {
        return defaultValue;
    }
}
