package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.fiftieshousewife.cleancode.recipes.DisabledTestRecipe;
import org.fiftieshousewife.cleancode.recipes.FlagArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.ImperativeLoopRecipe;
import org.fiftieshousewife.cleancode.recipes.LargeConstructorRecipe;
import org.fiftieshousewife.cleancode.recipes.LargeRecordRecipe;
import org.fiftieshousewife.cleancode.recipes.LawOfDemeterRecipe;
import org.fiftieshousewife.cleancode.recipes.MultipleAssertRecipe;
import org.fiftieshousewife.cleancode.recipes.OutputArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.PrivateMethodTestabilityRecipe;
import org.fiftieshousewife.cleancode.recipes.SectionCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.SelectorArgumentRecipe;
import org.fiftieshousewife.cleancode.recipes.StringlyTypedDispatchRecipe;
import org.fiftieshousewife.cleancode.recipes.WhitespaceSplitMethodRecipe;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Optional;

final class OpenRewriteFunctionMappers implements RecipeCategoryMapper {

    private final OpenRewriteFindingBuilder builder;

    OpenRewriteFunctionMappers(final OpenRewriteFindingBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Optional<List<Finding>> tryMap(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case FlagArgumentRecipe r -> Optional.of(flagArgs(r.collectedRows()));
            case OutputArgumentRecipe r -> Optional.of(outputArgs(r.collectedRows()));
            case LargeRecordRecipe r -> Optional.of(largeRecord(r.collectedRows()));
            case LargeConstructorRecipe r -> Optional.of(largeConstructor(r.collectedRows()));
            case WhitespaceSplitMethodRecipe r -> Optional.of(whitespaceSplit(r.collectedRows()));
            case ImperativeLoopRecipe r -> Optional.of(imperativeLoop(r.collectedRows()));
            case SectionCommentRecipe r -> Optional.of(sectionComment(r.collectedRows()));
            case SelectorArgumentRecipe r -> Optional.of(selectorArgument(r.collectedRows()));
            case StringlyTypedDispatchRecipe r -> Optional.of(stringlyTypedDispatch(r.collectedRows()));
            case MultipleAssertRecipe r -> Optional.of(multipleAssert(r.collectedRows()));
            case PrivateMethodTestabilityRecipe r -> Optional.of(privateMethod(r.collectedRows()));
            case DisabledTestRecipe r -> Optional.of(disabledTest(r.collectedRows()));
            case LawOfDemeterRecipe r -> Optional.of(demeter(r.collectedRows()));
            default -> Optional.empty();
        };
    }

    List<Finding> flagArgs(final List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.F3, r.className(),
                "Method '%s' takes boolean parameter '%s' — split into two methods instead"
                        .formatted(r.methodName(), r.paramName())));
    }

    List<Finding> outputArgs(final List<OutputArgumentRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.F2, r.className(), r.lineNumber(),
                "Method '%s' mutates its argument '%s' (%s) — return the result instead"
                        .formatted(r.methodName(), r.paramName(), r.paramType())));
    }

    List<Finding> largeRecord(final List<LargeRecordRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.F1, r.className(), r.lineNumber(),
                "Record '%s' has %d components — too many constructor parameters"
                        .formatted(r.className(), r.componentCount())));
    }

    List<Finding> largeConstructor(final List<LargeConstructorRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.F1, r.className(),
                "Constructor has %d parameters — introduce a parameter object or builder"
                        .formatted(r.parameterCount())));
    }

    List<Finding> whitespaceSplit(final List<WhitespaceSplitMethodRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                ("Method '%s' has %d blank-line sections across %d lines"
                        + " — each section should be its own method")
                        .formatted(r.methodName(), r.blankLineCount(), r.totalLines())));
    }

    List<Finding> imperativeLoop(final List<ImperativeLoopRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                "Loop in '%s' (%s) can be replaced with a stream operation"
                        .formatted(r.methodName(), r.loopPattern())));
    }

    List<Finding> sectionComment(final List<SectionCommentRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G34, r.className(), r.lineNumber(),
                "Method '%s' has %d section comments".formatted(r.methodName(), r.sectionCount())));
    }

    List<Finding> selectorArgument(final List<SelectorArgumentRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G15, r.className(),
                ("Method '%s' uses %s parameter '%s' to select behaviour"
                        + " — split into separate methods")
                        .formatted(r.methodName(), r.parameterType(), r.parameterName())));
    }

    List<Finding> stringlyTypedDispatch(final List<StringlyTypedDispatchRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G23, r.className(),
                ("Method '%s' dispatches on String parameter '%s' with %d branches"
                        + " — use an enum or split into separate methods")
                        .formatted(r.methodName(), r.parameterName(), r.branchCount())));
    }

    List<Finding> multipleAssert(final List<MultipleAssertRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.T1, r.className(),
                "Test '%s' has %d consecutive assertions — wrap in assertAll"
                        .formatted(r.methodName(), r.assertCount())));
    }

    List<Finding> privateMethod(
            final List<PrivateMethodTestabilityRecipe.PrivateMethodTestabilityRow> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.T1, r.className(), r.lineNumber(),
                ("Private method '%s' (%d lines) should be package-private"
                        + " so it can be tested directly")
                        .formatted(r.methodName(), r.lineCount())));
    }

    List<Finding> disabledTest(final List<DisabledTestRecipe.Row> rows) {
        return builder.mapRows(rows, r -> Finding.at(HeuristicCode.T3, r.className() + ".java", -1, -1,
                "@%s on '%s' without meaningful reason".formatted(r.annotation(), r.methodName()),
                Severity.INFO, Confidence.HIGH, OpenRewriteFindingBuilder.TOOL, "DisabledTestRecipe"));
    }

    List<Finding> demeter(final List<LawOfDemeterRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G36, r.className(),
                "Method chain depth %d: %s".formatted(r.depth(), r.chain())));
    }
}
