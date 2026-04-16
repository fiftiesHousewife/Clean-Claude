package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.RecipeThreshold;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.fiftieshousewife.cleancode.recipes.*;
import org.openrewrite.ScanningRecipe;

import java.util.List;

final class OpenRewriteRecipeFactory {

    private final RecipeThresholds thresholds;

    OpenRewriteRecipeFactory(final RecipeThresholds thresholds) {
        this.thresholds = thresholds;
    }

    List<ScanningRecipe<?>> createRecipes() {
        final List<ScanningRecipe<?>> recipes = new java.util.ArrayList<>();
        recipes.addAll(structuralRecipes());
        recipes.addAll(commentAndNamingRecipes());
        recipes.addAll(behaviouralRecipes());
        return List.copyOf(recipes);
    }

    private List<ScanningRecipe<?>> structuralRecipes() {
        return List.of(
                new FlagArgumentRecipe(),
                new OutputArgumentRecipe(),
                new CatchLogContinueRecipe(),
                new NegativeConditionalRecipe(),
                new LawOfDemeterRecipe(thresholds.get(RecipeThreshold.CHAIN_DEPTH_THRESHOLD)),
                new EncapsulateConditionalRecipe(),
                new NullDensityRecipe(thresholds.get(RecipeThreshold.NULL_CHECK_DENSITY)),
                new ClassLineLengthRecipe(thresholds.get(RecipeThreshold.CLASS_LINE_COUNT)),
                new LargeRecordRecipe(thresholds.get(RecipeThreshold.RECORD_COMPONENT_COUNT)),
                new DisabledTestRecipe(),
                new SwitchOnTypeRecipe(),
                new LargeConstructorRecipe(thresholds.get(RecipeThreshold.RECORD_COMPONENT_COUNT)),
                new VisibilityReductionRecipe(),
                new ImperativeLoopRecipe(),
                new UncheckedCastRecipe(),
                new FeatureEnvyRecipe(),
                new NestedTernaryRecipe());
    }

    private List<ScanningRecipe<?>> commentAndNamingRecipes() {
        return List.of(
                new CommentedCodeRecipe(),
                new MumblingCommentRecipe(),
                new SectionCommentRecipe(thresholds.get(RecipeThreshold.SECTION_COMMENT_THRESHOLD)),
                new EncodingNamingRecipe(),
                new VerticalSeparationRecipe(thresholds.get(RecipeThreshold.VERTICAL_SEPARATION_DISTANCE)),
                new InheritConstantsRecipe(),
                new EnumForConstantsRecipe(),
                new ShortVariableNameRecipe(thresholds.get(RecipeThreshold.SHORT_NAME_MIN_LENGTH)),
                new MagicStringRecipe(thresholds.get(RecipeThreshold.MAGIC_STRING_MIN_OCCURRENCES)),
                new WhitespaceSplitMethodRecipe(thresholds.get(RecipeThreshold.METHOD_BLANK_LINE_SECTIONS)),
                new PrivateMethodTestabilityRecipe(thresholds.get(RecipeThreshold.PRIVATE_METHOD_MIN_LINES)),
                new StringSwitchRecipe(thresholds.get(RecipeThreshold.STRING_SWITCH_MIN_CASES)),
                new SideEffectNamingRecipe(),
                new InconsistentNamingRecipe(),
                new BadClassNameRecipe(),
                new ObsoleteCommentRecipe(),
                new MissingExplanatoryVariableRecipe(),
                new BoundaryConditionRecipe());
    }

    private List<ScanningRecipe<?>> behaviouralRecipes() {
        return List.of(
                new SystemOutRecipe(),
                new LegacyFileApiRecipe(),
                new MultipleAssertRecipe(),
                new InappropriateStaticRecipe(),
                new StringlyTypedDispatchRecipe(),
                new ConfigurableDataRecipe(thresholds.get(RecipeThreshold.MAGIC_NUMBER_MIN_VALUE)),
                new EmbeddedLanguageRecipe(),
                new GuardClauseRecipe(),
                new BaseClassDependencyRecipe(),
                new ArtificialCouplingRecipe(),
                new HardcodedListRecipe(thresholds.get(RecipeThreshold.HARDCODED_LIST_MIN_LITERALS)),
                new SelectorArgumentRecipe(),
                new TemporalCouplingRecipe(thresholds.get(RecipeThreshold.TEMPORAL_COUPLING_MIN_CALLS)),
                new BroadCatchRecipe(),
                new RawGenericRecipe(),
                new SwallowedExceptionRecipe(),
                new InconsistentReturnRecipe(),
                new SuppressedWarningRecipe());
    }
}
