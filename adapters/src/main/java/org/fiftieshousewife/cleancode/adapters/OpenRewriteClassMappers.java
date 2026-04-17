package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.recipes.ArtificialCouplingRecipe;
import org.fiftieshousewife.cleancode.recipes.BadClassNameRecipe;
import org.fiftieshousewife.cleancode.recipes.BaseClassDependencyRecipe;
import org.fiftieshousewife.cleancode.recipes.ClassLineLengthRecipe;
import org.fiftieshousewife.cleancode.recipes.EncodingNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.FeatureEnvyRecipe;
import org.fiftieshousewife.cleancode.recipes.InappropriateStaticRecipe;
import org.fiftieshousewife.cleancode.recipes.InconsistentNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.InconsistentReturnRecipe;
import org.fiftieshousewife.cleancode.recipes.NestedTernaryRecipe;
import org.fiftieshousewife.cleancode.recipes.ShortVariableNameRecipe;
import org.fiftieshousewife.cleancode.recipes.SideEffectNamingRecipe;
import org.fiftieshousewife.cleancode.recipes.TemporalCouplingRecipe;
import org.fiftieshousewife.cleancode.recipes.VisibilityReductionRecipe;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Optional;

final class OpenRewriteClassMappers implements RecipeCategoryMapper {

    private final OpenRewriteFindingBuilder builder;

    OpenRewriteClassMappers(final OpenRewriteFindingBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Optional<List<Finding>> tryMap(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case ClassLineLengthRecipe r -> Optional.of(classLength(r.collectedRows()));
            case VisibilityReductionRecipe r -> Optional.of(visibility(r.collectedRows()));
            case FeatureEnvyRecipe r -> Optional.of(featureEnvy(r.collectedRows()));
            case InappropriateStaticRecipe r -> Optional.of(inappropriateStatic(r.collectedRows()));
            case BaseClassDependencyRecipe r -> Optional.of(baseClassDependency(r.collectedRows()));
            case ArtificialCouplingRecipe r -> Optional.of(artificialCoupling(r.collectedRows()));
            case BadClassNameRecipe r -> Optional.of(badClassName(r.collectedRows()));
            case ShortVariableNameRecipe r -> Optional.of(shortNames(r.collectedRows()));
            case EncodingNamingRecipe r -> Optional.of(encodingNaming(r.collectedRows()));
            case SideEffectNamingRecipe r -> Optional.of(sideEffectNaming(r.collectedRows()));
            case InconsistentNamingRecipe r -> Optional.of(inconsistentNaming(r.collectedRows()));
            case NestedTernaryRecipe r -> Optional.of(nestedTernary(r.collectedRows()));
            case InconsistentReturnRecipe r -> Optional.of(inconsistentReturn(r.collectedRows()));
            case TemporalCouplingRecipe r -> Optional.of(temporalCoupling(r.collectedRows()));
            default -> Optional.empty();
        };
    }

    List<Finding> classLength(final List<ClassLineLengthRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.Ch10_1, r.className(),
                "Class '%s' is %d lines".formatted(r.className(), r.lineCount())));
    }

    List<Finding> visibility(final List<VisibilityReductionRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G8, r.className(), r.lineNumber(),
                "Field '%s' is %s and mutable — should be private"
                        .formatted(r.fieldName(), r.currentVisibility())));
    }

    List<Finding> featureEnvy(final List<FeatureEnvyRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G14, r.className(), r.lineNumber(),
                ("Method '%s' calls %d methods on '%s' but only %d on its own class"
                        + " — it wants to live elsewhere")
                        .formatted(r.methodName(), r.externalCallCount(),
                                r.enviedClass(), r.selfCallCount())));
    }

    List<Finding> inappropriateStatic(final List<InappropriateStaticRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G18, r.className(),
                "Method '%s' does not use instance state — consider making it static or extracting"
                        .formatted(r.methodName())));
    }

    List<Finding> baseClassDependency(final List<BaseClassDependencyRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G7, r.className(),
                "'%s' depends on its derivative '%s' — invert the dependency"
                        .formatted(r.className(), r.derivativeName())));
    }

    List<Finding> artificialCoupling(final List<ArtificialCouplingRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G13, r.declaringClass(),
                "Constant '%s' defined in '%s' but only used in '%s' — move it"
                        .formatted(r.constantName(), r.declaringClass(), r.usedInClass())));
    }

    List<Finding> badClassName(final List<BadClassNameRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.N1, r.className(),
                "Class '%s' uses bad suffix '%s' — name after what it represents, not its role"
                        .formatted(r.className(), r.suffix())));
    }

    List<Finding> shortNames(final List<ShortVariableNameRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.N5, r.className(), r.lineNumber(),
                "'%s' in %s() is not a meaningful name — rename to reveal intent (%s)"
                        .formatted(r.variableName(), r.methodName(), r.context())));
    }

    List<Finding> encodingNaming(final List<EncodingNamingRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.N6, r.className(), r.lineNumber(),
                "%s '%s' uses %s".formatted(r.elementKind(), r.elementName(), r.violationType())));
    }

    List<Finding> sideEffectNaming(final List<SideEffectNamingRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.N7, r.className(), r.lineNumber(),
                "Method '%s' is named like a query but %s — rename to reveal the side effect"
                        .formatted(r.methodName(), r.sideEffect())));
    }

    List<Finding> inconsistentNaming(final List<InconsistentNamingRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G11, r.className(), r.lineNumber(),
                "Class uses inconsistent prefixes %s for the same concept: %s"
                        .formatted(r.conflictingPrefixes(), r.methodNames())));
    }

    List<Finding> nestedTernary(final List<NestedTernaryRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G16, r.className(), r.lineNumber(),
                "Ternary nested %d deep in '%s' — extract to an if/else or a named method"
                        .formatted(r.depth(), r.methodName())));
    }

    List<Finding> inconsistentReturn(final List<InconsistentReturnRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.F2, r.className(),
                ("Class has %d methods returning collections and %d void methods"
                        + " mutating collection params — pick one style")
                        .formatted(r.returningMethods(), r.mutatingMethods())));
    }

    List<Finding> temporalCoupling(final List<TemporalCouplingRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G31, r.className(),
                ("Method '%s' has %d consecutive void calls with no data dependency"
                        + " — make the order explicit")
                        .formatted(r.methodName(), r.callCount())));
    }
}
