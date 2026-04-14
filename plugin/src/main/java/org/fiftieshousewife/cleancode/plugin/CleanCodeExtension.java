package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@SuppressWarnings("this-escape")
public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    public abstract ListProperty<String> getDisabledRecipes();

    private final ThresholdsExtension thresholds;

    @Inject
    public CleanCodeExtension(ObjectFactory objects) {
        getFailOnViolation().convention(true);
        getDisabledRecipes().convention(java.util.List.of());
        thresholds = objects.newInstance(ThresholdsExtension.class);
    }

    public void thresholds(Action<? super ThresholdsExtension> action) {
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
                thresholds.getShortNameMinLength().get());
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
        }
    }
}
