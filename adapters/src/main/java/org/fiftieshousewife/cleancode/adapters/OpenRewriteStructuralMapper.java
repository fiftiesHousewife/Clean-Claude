package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.fiftieshousewife.cleancode.recipes.*;
import org.openrewrite.ScanningRecipe;

import java.util.List;

final class OpenRewriteStructuralMapper {

    private final OpenRewriteFindingFactory findings;

    OpenRewriteStructuralMapper(final OpenRewriteFindingFactory findings) {
        this.findings = findings;
    }

    List<Finding> map(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case FlagArgumentRecipe r -> mapFlagArgs(r.collectedRows());
            case OutputArgumentRecipe r -> mapOutputArgs(r.collectedRows());
            case CatchLogContinueRecipe r -> mapCatchLog(r.collectedRows());
            case NegativeConditionalRecipe r -> mapNegCond(r.collectedRows());
            case LawOfDemeterRecipe r -> mapDemeter(r.collectedRows());
            case EncapsulateConditionalRecipe r -> mapEncapCond(r.collectedRows());
            case NullDensityRecipe r -> mapNullDensity(r.collectedRows());
            case ClassLineLengthRecipe r -> mapClassLength(r.collectedRows());
            case LargeRecordRecipe r -> mapLargeRecord(r.collectedRows());
            case DisabledTestRecipe r -> mapDisabledTest(r.collectedRows());
            case SwitchOnTypeRecipe r -> mapSwitchOnType(r.collectedRows());
            case LargeConstructorRecipe r -> mapLargeConstructor(r.collectedRows());
            case VisibilityReductionRecipe r -> mapVisibility(r.collectedRows());
            case ImperativeLoopRecipe r -> mapImperativeLoop(r.collectedRows());
            case UncheckedCastRecipe r -> mapUncheckedCast(r.collectedRows());
            case FeatureEnvyRecipe r -> mapFeatureEnvy(r.collectedRows());
            case NestedTernaryRecipe r -> mapNestedTernary(r.collectedRows());
            default -> List.of();
        };
    }

    private List<Finding> mapFlagArgs(final List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.F3, r.className(),
                        flagArgMessage(r.methodName(), r.paramName())))
                .toList();
    }

    private static String flagArgMessage(final String methodName, final String paramName) {
        return "Method '%s' takes boolean parameter '%s' — split into two methods instead"
                .formatted(methodName, paramName);
    }

    private List<Finding> mapOutputArgs(final List<OutputArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.F2, r.className(), r.lineNumber(),
                        outputArgMessage(r.methodName(), r.paramName(), r.paramType())))
                .toList();
    }

    private static String outputArgMessage(final String methodName, final String paramName, final String paramType) {
        return "Method '%s' mutates its argument '%s' (%s) — return the result instead"
                .formatted(methodName, paramName, paramType);
    }

    private List<Finding> mapCatchLog(final List<CatchLogContinueRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.Ch7_1, r.className(),
                        "Catch block in '%s' only logs or is empty".formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapNegCond(final List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G29, r.className(),
                        "Double negation: %s".formatted(r.expression())))
                .toList();
    }

    private List<Finding> mapDemeter(final List<LawOfDemeterRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G36, r.className(),
                        "Method chain depth %d: %s".formatted(r.depth(), r.chain())))
                .toList();
    }

    private List<Finding> mapEncapCond(final List<EncapsulateConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G28, r.className(),
                        "Complex condition (depth %d) should be extracted".formatted(r.depth())))
                .toList();
    }

    private List<Finding> mapNullDensity(final List<NullDensityRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.Ch7_2, r.className(),
                        "Method '%s' has %d null checks".formatted(r.methodName(), r.nullCheckCount())))
                .toList();
    }

    private List<Finding> mapClassLength(final List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapLargeRecord(final List<LargeRecordRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.F1, r.className(), r.lineNumber(),
                        largeRecordMessage(r.className(), r.componentCount())))
                .toList();
    }

    private static String largeRecordMessage(final String className, final int componentCount) {
        return "Record '%s' has %d components — too many constructor parameters"
                .formatted(className, componentCount);
    }

    private List<Finding> mapDisabledTest(final List<DisabledTestRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.T3, r.className() + ".java", -1, -1,
                        "@%s on '%s' without meaningful reason".formatted(r.annotation(), r.methodName()),
                        Severity.INFO, Confidence.HIGH, OpenRewriteFindingFactory.TOOL, "DisabledTestRecipe"))
                .toList();
    }

    private List<Finding> mapSwitchOnType(final List<SwitchOnTypeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        "Type switch in '%s': %s".formatted(r.methodName(), r.pattern())))
                .toList();
    }

    private List<Finding> mapLargeConstructor(final List<LargeConstructorRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.F1, r.className(),
                        "Constructor has %d parameters — introduce a parameter object or builder"
                                .formatted(r.parameterCount())))
                .toList();
    }

    private List<Finding> mapVisibility(final List<VisibilityReductionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G8, r.className(), r.lineNumber(),
                        "Field '%s' is %s and mutable — should be private"
                                .formatted(r.fieldName(), r.currentVisibility())))
                .toList();
    }

    private List<Finding> mapImperativeLoop(final List<ImperativeLoopRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        "Loop in '%s' (%s) can be replaced with a stream operation"
                                .formatted(r.methodName(), r.loopPattern())))
                .toList();
    }

    private List<Finding> mapUncheckedCast(final List<UncheckedCastRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G4, r.className(), r.lineNumber(),
                        "@SuppressWarnings(\"unchecked\") on '%s' — redesign to avoid unsafe casts"
                                .formatted(r.memberName())))
                .toList();
    }

    private List<Finding> mapFeatureEnvy(final List<FeatureEnvyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G14, r.className(), r.lineNumber(),
                        featureEnvyMessage(r)))
                .toList();
    }

    private static String featureEnvyMessage(final FeatureEnvyRecipe.Row r) {
        return ("Method '%s' calls %d methods on '%s' but only %d on its own class"
                + " — it wants to live elsewhere")
                .formatted(r.methodName(), r.externalCallCount(), r.enviedClass(), r.selfCallCount());
    }

    private List<Finding> mapNestedTernary(final List<NestedTernaryRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G16, r.className(), r.lineNumber(),
                        "Ternary nested %d deep in '%s' — extract to an if/else or a named method"
                                .formatted(r.depth(), r.methodName())))
                .toList();
    }
}
