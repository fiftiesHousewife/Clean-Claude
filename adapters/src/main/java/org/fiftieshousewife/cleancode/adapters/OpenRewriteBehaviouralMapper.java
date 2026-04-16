package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.recipes.*;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Locale;

final class OpenRewriteBehaviouralMapper {

    private final OpenRewriteFindingFactory findings;

    OpenRewriteBehaviouralMapper(final OpenRewriteFindingFactory findings) {
        this.findings = findings;
    }

    List<Finding> map(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case SystemOutRecipe r -> mapSystemOut(r.collectedRows());
            case LegacyFileApiRecipe r -> mapLegacyFileApi(r.collectedRows());
            case MultipleAssertRecipe r -> mapMultipleAssert(r.collectedRows());
            case InappropriateStaticRecipe r -> mapInappropriateStatic(r.collectedRows());
            case StringlyTypedDispatchRecipe r -> mapStringlyTypedDispatch(r.collectedRows());
            case ConfigurableDataRecipe r -> mapConfigurableData(r.collectedRows());
            case EmbeddedLanguageRecipe r -> mapEmbeddedLanguage(r.collectedRows());
            case GuardClauseRecipe r -> mapGuardClause(r.collectedRows());
            case BaseClassDependencyRecipe r -> mapBaseClassDependency(r.collectedRows());
            case ArtificialCouplingRecipe r -> mapArtificialCoupling(r.collectedRows());
            case HardcodedListRecipe r -> mapHardcodedList(r.collectedRows());
            case SelectorArgumentRecipe r -> mapSelectorArgument(r.collectedRows());
            case TemporalCouplingRecipe r -> mapTemporalCoupling(r.collectedRows());
            case BroadCatchRecipe r -> mapBroadCatch(r.collectedRows());
            case RawGenericRecipe r -> mapRawGeneric(r.collectedRows());
            case SwallowedExceptionRecipe r -> mapSwallowedException(r.collectedRows());
            case InconsistentReturnRecipe r -> mapInconsistentReturn(r.collectedRows());
            case SuppressedWarningRecipe r -> mapSuppressedWarning(r.collectedRows());
            default -> List.of();
        };
    }

    private List<Finding> mapSystemOut(final List<SystemOutRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G4, r.className(),
                        "'%s' bypasses structured logging — use @Slf4j instead".formatted(r.call())))
                .toList();
    }

    private List<Finding> mapLegacyFileApi(final List<LegacyFileApiRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G26, r.className(),
                        "'%s' is a legacy API — use java.nio.file.Path and Files instead"
                                .formatted(r.legacyType())))
                .toList();
    }

    private List<Finding> mapMultipleAssert(final List<MultipleAssertRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.T1, r.className(),
                        "Test '%s' has %d consecutive assertions — wrap in assertAll"
                                .formatted(r.methodName(), r.assertCount())))
                .toList();
    }

    private List<Finding> mapInappropriateStatic(final List<InappropriateStaticRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G18, r.className(),
                        "Method '%s' does not use instance state — consider making it static or extracting"
                                .formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapStringlyTypedDispatch(final List<StringlyTypedDispatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G23, r.className(),
                        stringlyTypedDispatchMessage(r)))
                .toList();
    }

    private static String stringlyTypedDispatchMessage(final StringlyTypedDispatchRecipe.Row r) {
        return ("Method '%s' dispatches on String parameter '%s' with %d branches"
                + " — use an enum or split into separate methods")
                .formatted(r.methodName(), r.parameterName(), r.branchCount());
    }

    private List<Finding> mapConfigurableData(final List<ConfigurableDataRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G35, r.className(),
                        "Magic number %s in private method '%s' — extract to a named constant"
                                .formatted(r.literalValue(), r.methodName())))
                .toList();
    }

    private List<Finding> mapEmbeddedLanguage(final List<EmbeddedLanguageRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G1, r.className(),
                        "Embedded %s in method '%s' — extract to a template or resource file"
                                .formatted(r.language().toUpperCase(Locale.ROOT), r.methodName())))
                .toList();
    }

    private List<Finding> mapGuardClause(final List<GuardClauseRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G29, r.className(),
                        "Method '%s' has %d guard clauses — simplify with early return or extract filter"
                                .formatted(r.methodName(), r.guardCount())))
                .toList();
    }

    private List<Finding> mapBaseClassDependency(final List<BaseClassDependencyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G7, r.className(),
                        "'%s' depends on its derivative '%s' — invert the dependency"
                                .formatted(r.className(), r.derivativeName())))
                .toList();
    }

    private List<Finding> mapArtificialCoupling(final List<ArtificialCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G13, r.declaringClass(),
                        "Constant '%s' defined in '%s' but only used in '%s' — move it"
                                .formatted(r.constantName(), r.declaringClass(), r.usedInClass())))
                .toList();
    }

    private List<Finding> mapHardcodedList(final List<HardcodedListRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G35, r.className(),
                        "Field '%s' has %d hardcoded literals — load from configuration"
                                .formatted(r.fieldName(), r.literalCount())))
                .toList();
    }

    private List<Finding> mapSelectorArgument(final List<SelectorArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G15, r.className(),
                        selectorArgumentMessage(r)))
                .toList();
    }

    private static String selectorArgumentMessage(final SelectorArgumentRecipe.Row r) {
        return ("Method '%s' uses %s parameter '%s' to select behaviour"
                + " — split into separate methods")
                .formatted(r.methodName(), r.parameterType(), r.parameterName());
    }

    private List<Finding> mapTemporalCoupling(final List<TemporalCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G31, r.className(),
                        temporalCouplingMessage(r.methodName(), r.callCount())))
                .toList();
    }

    private static String temporalCouplingMessage(final String methodName, final int callCount) {
        return ("Method '%s' has %d consecutive void calls with no data dependency"
                + " — make the order explicit")
                .formatted(methodName, callCount);
    }

    private List<Finding> mapBroadCatch(final List<BroadCatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.Ch7_1, r.className(),
                        "Method '%s' catches %s — catch specific exception types instead"
                                .formatted(r.methodName(), r.caughtType())))
                .toList();
    }

    private List<Finding> mapRawGeneric(final List<RawGenericRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G26, r.className(),
                        "'%s' in '%s' uses Object type parameter — use a typed record or specific generic"
                                .formatted(r.typeName(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSwallowedException(final List<SwallowedExceptionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G4, r.className(),
                        "Method '%s' catches %s and silently swallows it — handle or propagate"
                                .formatted(r.methodName(), r.exceptionType())))
                .toList();
    }

    private List<Finding> mapInconsistentReturn(final List<InconsistentReturnRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.F2, r.className(),
                        inconsistentReturnMessage(r.returningMethods(), r.mutatingMethods())))
                .toList();
    }

    private static String inconsistentReturnMessage(final int returningMethods, final int mutatingMethods) {
        return ("Class has %d methods returning collections and %d void methods"
                + " mutating collection params — pick one style")
                .formatted(returningMethods, mutatingMethods);
    }

    private List<Finding> mapSuppressedWarning(final List<SuppressedWarningRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G4, r.className(),
                        "@SuppressWarnings(\"%s\") on '%s' — redesign to avoid unsafe operations"
                                .formatted(r.warningType(), r.methodName())))
                .toList();
    }
}
