package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.recipes.BoundaryConditionRecipe;
import org.fiftieshousewife.cleancode.recipes.ConfigurableDataRecipe;
import org.fiftieshousewife.cleancode.recipes.EncapsulateConditionalRecipe;
import org.fiftieshousewife.cleancode.recipes.EnumForConstantsRecipe;
import org.fiftieshousewife.cleancode.recipes.GuardClauseRecipe;
import org.fiftieshousewife.cleancode.recipes.HardcodedListRecipe;
import org.fiftieshousewife.cleancode.recipes.InheritConstantsRecipe;
import org.fiftieshousewife.cleancode.recipes.MagicStringRecipe;
import org.fiftieshousewife.cleancode.recipes.MissingExplanatoryVariableRecipe;
import org.fiftieshousewife.cleancode.recipes.NegativeConditionalRecipe;
import org.fiftieshousewife.cleancode.recipes.NullDensityRecipe;
import org.fiftieshousewife.cleancode.recipes.StringSwitchRecipe;
import org.fiftieshousewife.cleancode.recipes.SwitchOnTypeRecipe;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Optional;

final class OpenRewriteConditionalMappers implements RecipeCategoryMapper {

    private final OpenRewriteFindingBuilder builder;

    OpenRewriteConditionalMappers(final OpenRewriteFindingBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Optional<List<Finding>> tryMap(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case NegativeConditionalRecipe r -> Optional.of(negCond(r.collectedRows()));
            case GuardClauseRecipe r -> Optional.of(guardClause(r.collectedRows()));
            case EncapsulateConditionalRecipe r -> Optional.of(encapCond(r.collectedRows()));
            case SwitchOnTypeRecipe r -> Optional.of(switchOnType(r.collectedRows()));
            case StringSwitchRecipe r -> Optional.of(stringSwitch(r.collectedRows()));
            case BoundaryConditionRecipe r -> Optional.of(boundaryCondition(r.collectedRows()));
            case MissingExplanatoryVariableRecipe r -> Optional.of(missingExplanatory(r.collectedRows()));
            case NullDensityRecipe r -> Optional.of(nullDensity(r.collectedRows()));
            case EnumForConstantsRecipe r -> Optional.of(enumForConstants(r.collectedRows()));
            case InheritConstantsRecipe r -> Optional.of(inheritConstants(r.collectedRows()));
            case MagicStringRecipe r -> Optional.of(magicStrings(r.collectedRows()));
            case ConfigurableDataRecipe r -> Optional.of(configurableData(r.collectedRows()));
            case HardcodedListRecipe r -> Optional.of(hardcodedList(r.collectedRows()));
            default -> Optional.empty();
        };
    }

    List<Finding> negCond(final List<NegativeConditionalRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G29, r.className(),
                "Double negation: %s".formatted(r.expression())));
    }

    List<Finding> guardClause(final List<GuardClauseRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G29, r.className(),
                "Method '%s' has %d guard clauses — simplify with early return or extract filter"
                        .formatted(r.methodName(), r.guardCount())));
    }

    List<Finding> encapCond(final List<EncapsulateConditionalRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G28, r.className(),
                "Complex condition (depth %d) should be extracted".formatted(r.depth())));
    }

    List<Finding> switchOnType(final List<SwitchOnTypeRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                "Type switch in '%s': %s".formatted(r.methodName(), r.pattern())));
    }

    List<Finding> stringSwitch(final List<StringSwitchRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                ("Switch on String '%s' with %d cases"
                        + " — replace with an enum that encapsulates the behaviour")
                        .formatted(r.selectorName(), r.caseCount())));
    }

    List<Finding> boundaryCondition(final List<BoundaryConditionRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G33, r.className(), r.lineNumber(),
                "Boundary adjustment '%s' in '%s' — extract to a named variable"
                        .formatted(r.expression(), r.methodName())));
    }

    List<Finding> missingExplanatory(final List<MissingExplanatoryVariableRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G19, r.className(), r.lineNumber(),
                "Complex expression in '%s' should be extracted to a named variable: %s"
                        .formatted(r.methodName(), r.expressionPreview())));
    }

    List<Finding> nullDensity(final List<NullDensityRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.Ch7_2, r.className(),
                "Method '%s' has %d null checks".formatted(r.methodName(), r.nullCheckCount())));
    }

    List<Finding> enumForConstants(final List<EnumForConstantsRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.J3, r.className(), r.lineNumber(),
                "%d static final fields with prefix '%s' should be an enum"
                        .formatted(r.fieldCount(), r.prefix())));
    }

    List<Finding> inheritConstants(final List<InheritConstantsRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.J2, r.className(), r.lineNumber(),
                "Class inherits constants from interface '%s'".formatted(r.interfaceName())));
    }

    List<Finding> magicStrings(final List<MagicStringRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G25, r.className(), r.lineNumber(),
                "String \"%s\" appears %d times — extract to a named constant"
                        .formatted(r.value(), r.count())));
    }

    List<Finding> configurableData(final List<ConfigurableDataRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G35, r.className(),
                "Magic number %s in private method '%s' — extract to a named constant"
                        .formatted(r.literalValue(), r.methodName())));
    }

    List<Finding> hardcodedList(final List<HardcodedListRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G35, r.className(),
                "Field '%s' has %d hardcoded literals — load from configuration"
                        .formatted(r.fieldName(), r.literalCount())));
    }
}
