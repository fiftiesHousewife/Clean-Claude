package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ThresholdsExtensionTest {

    private ThresholdsExtension thresholds;

    @BeforeEach
    void setUp() {
        final Project project = ProjectBuilder.builder().build();
        thresholds = project.getObjects().newInstance(ThresholdsExtension.class);
    }

    @Test
    void appliesDefaultConventionsForEveryThreshold() {
        assertAll(
                () -> assertEquals(RecipeThresholds.DEFAULT_CLASS_LINE_COUNT,
                        thresholds.getClassLineCount().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_RECORD_COMPONENT_COUNT,
                        thresholds.getRecordComponentCount().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_NULL_CHECK_DENSITY,
                        thresholds.getNullCheckDensity().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_CHAIN_DEPTH_THRESHOLD,
                        thresholds.getChainDepthThreshold().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_VERTICAL_SEPARATION_DISTANCE,
                        thresholds.getVerticalSeparationDistance().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_METHOD_BLANK_LINE_SECTIONS,
                        thresholds.getMethodBlankLineSections().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_PRIVATE_METHOD_MIN_LINES,
                        thresholds.getPrivateMethodMinLines().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_MAGIC_STRING_MIN_OCCURRENCES,
                        thresholds.getMagicStringMinOccurrences().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_STRING_SWITCH_MIN_CASES,
                        thresholds.getStringSwitchMinCases().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_SHORT_NAME_MIN_LENGTH,
                        thresholds.getShortNameMinLength().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_CPD_MINIMUM_TOKENS,
                        thresholds.getCpdMinimumTokens().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_MAGIC_NUMBER_MIN_VALUE,
                        thresholds.getMagicNumberMinValue().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_SECTION_COMMENT_THRESHOLD,
                        thresholds.getSectionCommentThreshold().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_HARDCODED_LIST_MIN_LITERALS,
                        thresholds.getHardcodedListMinLiterals().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_TEMPORAL_COUPLING_MIN_CALLS,
                        thresholds.getTemporalCouplingMinCalls().get()),
                () -> assertEquals(RecipeThresholds.DEFAULT_LINE_LENGTH_ERROR_THRESHOLD,
                        thresholds.getLineLengthErrorThreshold().get())
        );
    }

    @Test
    void allowsOverridingEachThreshold() {
        thresholds.getClassLineCount().set(200);
        thresholds.getRecordComponentCount().set(8);
        thresholds.getCpdMinimumTokens().set(100);
        thresholds.getLineLengthErrorThreshold().set(180);

        assertAll(
                () -> assertEquals(200, thresholds.getClassLineCount().get()),
                () -> assertEquals(8, thresholds.getRecordComponentCount().get()),
                () -> assertEquals(100, thresholds.getCpdMinimumTokens().get()),
                () -> assertEquals(180, thresholds.getLineLengthErrorThreshold().get())
        );
    }
}
