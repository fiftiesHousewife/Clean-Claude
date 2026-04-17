package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.fiftieshousewife.cleancode.recipes.ArtificialCouplingRecipe;
import org.fiftieshousewife.cleancode.recipes.BadClassNameRecipe;
import org.fiftieshousewife.cleancode.recipes.BaseClassDependencyRecipe;
import org.fiftieshousewife.cleancode.recipes.BoundaryConditionRecipe;
import org.fiftieshousewife.cleancode.recipes.BroadCatchRecipe;
import org.fiftieshousewife.cleancode.recipes.CatchLogContinueRecipe;
import org.fiftieshousewife.cleancode.recipes.ClassLineLengthRecipe;
import org.fiftieshousewife.cleancode.recipes.CommentedCodeRecipe;
import org.fiftieshousewife.cleancode.recipes.ConfigurableDataRecipe;
import org.fiftieshousewife.cleancode.recipes.DisabledTestRecipe;
import org.fiftieshousewife.cleancode.recipes.EmbeddedLanguageRecipe;
import org.fiftieshousewife.cleancode.recipes.EncapsulateConditionalRecipe;
import org.fiftieshousewife.cleancode.recipes.EncodingNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.EnumForConstantsRecipe;
import org.fiftieshousewife.cleancode.recipes.FeatureEnvyRecipe;
import org.fiftieshousewife.cleancode.recipes.FlagArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.FullyQualifiedReferenceRecipe;
import org.fiftieshousewife.cleancode.recipes.GuardClauseRecipe;
import org.fiftieshousewife.cleancode.recipes.HardcodedListRecipe;
import org.fiftieshousewife.cleancode.recipes.ImperativeLoopRecipe;
import org.fiftieshousewife.cleancode.recipes.InappropriateStaticRecipe;
import org.fiftieshousewife.cleancode.recipes.InconsistentNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.InconsistentReturnRecipe;
import org.fiftieshousewife.cleancode.recipes.InheritConstantsRecipe;
import org.fiftieshousewife.cleancode.recipes.LargeConstructorRecipe;
import org.fiftieshousewife.cleancode.recipes.LargeRecordRecipe;
import org.fiftieshousewife.cleancode.recipes.LawOfDemeterRecipe;
import org.fiftieshousewife.cleancode.recipes.LegacyFileApiRecipe;
import org.fiftieshousewife.cleancode.recipes.MagicStringRecipe;
import org.fiftieshousewife.cleancode.recipes.MissingExplanatoryVariableRecipe;
import org.fiftieshousewife.cleancode.recipes.MultipleAssertRecipe;
import org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.NegativeConditionalRecipe;
import org.fiftieshousewife.cleancode.recipes.NestedTernaryRecipe;
import org.fiftieshousewife.cleancode.recipes.NullDensityRecipe;
import org.fiftieshousewife.cleancode.recipes.ObsoleteCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.OutputArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.PrivateMethodTestabilityRecipe;
import org.fiftieshousewife.cleancode.recipes.RawGenericRecipe;
import org.fiftieshousewife.cleancode.recipes.SectionCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.SelectorArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.ShortVariableNameRecipe;
import org.fiftieshousewife.cleancode.recipes.SideEffectNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.StringSwitchRecipe;
import org.fiftieshousewife.cleancode.recipes.StringlyTypedDispatchRecipe;
import org.fiftieshousewife.cleancode.recipes.SuppressedWarningRecipe;
import org.fiftieshousewife.cleancode.recipes.SwallowedExceptionRecipe;
import org.fiftieshousewife.cleancode.recipes.SwitchOnTypeRecipe;
import org.fiftieshousewife.cleancode.recipes.SystemOutRecipe;
import org.fiftieshousewife.cleancode.recipes.TemporalCouplingRecipe;
import org.fiftieshousewife.cleancode.recipes.UncheckedCastRecipe;
import org.fiftieshousewife.cleancode.recipes.VerticalSeparationRecipe;
import org.fiftieshousewife.cleancode.recipes.VisibilityReductionRecipe;
import org.fiftieshousewife.cleancode.recipes.WhitespaceSplitMethodRecipe;
import org.openrewrite.ScanningRecipe;

import java.util.ArrayList;
import java.util.List;

final class RecipeCatalog {

    private final RecipeThresholds thresholds;

    RecipeCatalog(final RecipeThresholds thresholds) {
        this.thresholds = thresholds;
    }

    List<ScanningRecipe<?>> all() {
        final List<ScanningRecipe<?>> recipes = new ArrayList<>();
        recipes.addAll(argumentAndOutputRecipes());
        recipes.addAll(conditionalAndExpressionRecipes());
        recipes.addAll(sizeAndShapeRecipes());
        recipes.addAll(commentAndNamingRecipes());
        recipes.addAll(controlFlowRecipes());
        recipes.addAll(exceptionAndSafetyRecipes());
        recipes.addAll(couplingAndStructureRecipes());
        return recipes;
    }

    private List<ScanningRecipe<?>> argumentAndOutputRecipes() {
        return List.of(
                new FlagArgumentRecipe(),
                new OutputArgumentRecipe(),
                new SelectorArgumentRecipe(),
                new StringlyTypedDispatchRecipe(),
                new LargeConstructorRecipe(thresholds.recordComponentCount()),
                new LargeRecordRecipe(thresholds.recordComponentCount()),
                new InconsistentReturnRecipe());
    }

    private List<ScanningRecipe<?>> conditionalAndExpressionRecipes() {
        return List.of(
                new NegativeConditionalRecipe(),
                new EncapsulateConditionalRecipe(),
                new NullDensityRecipe(thresholds.nullCheckDensity()),
                new SwitchOnTypeRecipe(),
                new StringSwitchRecipe(thresholds.stringSwitchMinCases()),
                new NestedTernaryRecipe(),
                new MissingExplanatoryVariableRecipe(),
                new BoundaryConditionRecipe(),
                new GuardClauseRecipe());
    }

    private List<ScanningRecipe<?>> sizeAndShapeRecipes() {
        return List.of(
                new ClassLineLengthRecipe(thresholds.classLineCount()),
                new WhitespaceSplitMethodRecipe(thresholds.methodBlankLineSections()),
                new PrivateMethodTestabilityRecipe(thresholds.privateMethodMinLines()),
                new SectionCommentRecipe(thresholds.sectionCommentThreshold()),
                new VerticalSeparationRecipe(thresholds.verticalSeparationDistance()),
                new LawOfDemeterRecipe(thresholds.chainDepthThreshold()));
    }

    private List<ScanningRecipe<?>> commentAndNamingRecipes() {
        return List.of(
                new CommentedCodeRecipe(),
                new MumblingCommentRecipe(),
                new ObsoleteCommentRecipe(),
                new EncodingNamingRecipe(),
                new ShortVariableNameRecipe(thresholds.shortNameMinLength()),
                new SideEffectNamingRecipe(),
                new InconsistentNamingRecipe(),
                new BadClassNameRecipe(),
                new DisabledTestRecipe(),
                new MultipleAssertRecipe());
    }

    private List<ScanningRecipe<?>> controlFlowRecipes() {
        return List.of(
                new ImperativeLoopRecipe(),
                new VisibilityReductionRecipe(),
                new InappropriateStaticRecipe(),
                new TemporalCouplingRecipe(thresholds.temporalCouplingMinCalls()),
                new FeatureEnvyRecipe());
    }

    private List<ScanningRecipe<?>> exceptionAndSafetyRecipes() {
        return List.of(
                new CatchLogContinueRecipe(),
                new BroadCatchRecipe(),
                new SwallowedExceptionRecipe(),
                new UncheckedCastRecipe(),
                new SuppressedWarningRecipe(),
                new SystemOutRecipe(),
                new LegacyFileApiRecipe(),
                new RawGenericRecipe());
    }

    private List<ScanningRecipe<?>> couplingAndStructureRecipes() {
        return List.of(
                new InheritConstantsRecipe(),
                new EnumForConstantsRecipe(),
                new MagicStringRecipe(thresholds.magicStringMinOccurrences()),
                new ConfigurableDataRecipe(thresholds.magicNumberMinValue()),
                new HardcodedListRecipe(thresholds.hardcodedListMinLiterals()),
                new EmbeddedLanguageRecipe(),
                new BaseClassDependencyRecipe(),
                new ArtificialCouplingRecipe(),
                new FullyQualifiedReferenceRecipe());
    }
}
