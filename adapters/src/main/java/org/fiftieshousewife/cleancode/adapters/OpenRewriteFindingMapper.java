package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
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
import org.openrewrite.SourceFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class OpenRewriteFindingMapper {

    private static final String TOOL = "openrewrite";

    private final Map<String, String> classNameToSourcePath;

    private OpenRewriteFindingMapper(final Map<String, String> classNameToSourcePath) {
        this.classNameToSourcePath = classNameToSourcePath;
    }

    static OpenRewriteFindingMapper forSourceFiles(final List<SourceFile> parsed) {
        return new OpenRewriteFindingMapper(buildSourcePathIndex(parsed));
    }

    List<Finding> extractFindings(final List<ScanningRecipe<?>> recipes) {
        final List<Finding> findings = new ArrayList<>();
        recipes.forEach(recipe -> findings.addAll(mapRecipe(recipe)));
        return findings;
    }

    private List<Finding> mapRecipe(final ScanningRecipe<?> recipe) {
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
            case CommentedCodeRecipe r -> mapCommentedCode(r.collectedRows());
            case MumblingCommentRecipe r -> mapMumblingComment(r.collectedRows());
            case SectionCommentRecipe r -> mapSectionComment(r.collectedRows());
            case EncodingNamingRecipe r -> mapEncodingNaming(r.collectedRows());
            case VerticalSeparationRecipe r -> mapVerticalSeparation(r.collectedRows());
            case InheritConstantsRecipe r -> mapInheritConstants(r.collectedRows());
            case EnumForConstantsRecipe r -> mapEnumForConstants(r.collectedRows());
            case ShortVariableNameRecipe r -> mapShortNames(r.collectedRows());
            case MagicStringRecipe r -> mapMagicStrings(r.collectedRows());
            case WhitespaceSplitMethodRecipe r -> mapWhitespaceSplit(r.collectedRows());
            case PrivateMethodTestabilityRecipe r -> mapPrivateMethod(r.collectedRows());
            case StringSwitchRecipe r -> mapStringSwitch(r.collectedRows());
            case VisibilityReductionRecipe r -> mapVisibility(r.collectedRows());
            case ImperativeLoopRecipe r -> mapImperativeLoop(r.collectedRows());
            case UncheckedCastRecipe r -> mapUncheckedCast(r.collectedRows());
            case FeatureEnvyRecipe r -> mapFeatureEnvy(r.collectedRows());
            case NestedTernaryRecipe r -> mapNestedTernary(r.collectedRows());
            case MissingExplanatoryVariableRecipe r -> mapMissingExplanatory(r.collectedRows());
            case BoundaryConditionRecipe r -> mapBoundaryCondition(r.collectedRows());
            case SideEffectNamingRecipe r -> mapSideEffectNaming(r.collectedRows());
            case InconsistentNamingRecipe r -> mapInconsistentNaming(r.collectedRows());
            case BadClassNameRecipe r -> mapBadClassName(r.collectedRows());
            case SystemOutRecipe r -> mapSystemOut(r.collectedRows());
            case LegacyFileApiRecipe r -> mapLegacyFileApi(r.collectedRows());
            case MultipleAssertRecipe r -> mapMultipleAssert(r.collectedRows());
            case LargeConstructorRecipe r -> mapLargeConstructor(r.collectedRows());
            case InappropriateStaticRecipe r -> mapInappropriateStatic(r.collectedRows());
            case StringlyTypedDispatchRecipe r -> mapStringlyTypedDispatch(r.collectedRows());
            case ConfigurableDataRecipe r -> mapConfigurableData(r.collectedRows());
            case EmbeddedLanguageRecipe r -> mapEmbeddedLanguage(r.collectedRows());
            case GuardClauseRecipe r -> mapGuardClause(r.collectedRows());
            case BaseClassDependencyRecipe r -> mapBaseClassDependency(r.collectedRows());
            case ArtificialCouplingRecipe r -> mapArtificialCoupling(r.collectedRows());
            case HardcodedListRecipe r -> mapHardcodedList(r.collectedRows());
            case SelectorArgumentRecipe r -> mapSelectorArgument(r.collectedRows());
            case ObsoleteCommentRecipe r -> mapObsoleteComment(r.collectedRows());
            case TemporalCouplingRecipe r -> mapTemporalCoupling(r.collectedRows());
            case BroadCatchRecipe r -> mapBroadCatch(r.collectedRows());
            case RawGenericRecipe r -> mapRawGeneric(r.collectedRows());
            case SwallowedExceptionRecipe r -> mapSwallowedException(r.collectedRows());
            case InconsistentReturnRecipe r -> mapInconsistentReturn(r.collectedRows());
            case SuppressedWarningRecipe r -> mapSuppressedWarning(r.collectedRows());
            case FullyQualifiedReferenceRecipe r -> mapFullyQualifiedReferences(r.collectedRows());
            default -> List.of();
        };
    }

    private List<Finding> mapFullyQualifiedReferences(final List<FullyQualifiedReferenceRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.G12, r.sourceFile(), 0, 0,
                        ("%d inline fully-qualified type reference(s); first: %s"
                                + " — run ShortenFullyQualifiedReferencesRecipe")
                                .formatted(r.count(), r.samplePreview()),
                        Severity.WARNING, Confidence.HIGH, TOOL, "FullyQualifiedReferenceRecipe"))
                .toList();
    }

    private List<Finding> mapFlagArgs(final List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F3, r.className(),
                        "Method '%s' takes boolean parameter '%s' — split into two methods instead"
                                .formatted(r.methodName(), r.paramName())))
                .toList();
    }

    private List<Finding> mapOutputArgs(final List<OutputArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F2, r.className(), r.lineNumber(),
                        "Method '%s' mutates its argument '%s' (%s) — return the result instead"
                                .formatted(r.methodName(), r.paramName(), r.paramType())))
                .toList();
    }

    private List<Finding> mapCatchLog(final List<CatchLogContinueRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_1, r.className(),
                        "Catch block in '%s' only logs or is empty".formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapNegCond(final List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(),
                        "Double negation: %s".formatted(r.expression())))
                .toList();
    }

    private List<Finding> mapDemeter(final List<LawOfDemeterRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G36, r.className(),
                        "Method chain depth %d: %s".formatted(r.depth(), r.chain())))
                .toList();
    }

    private List<Finding> mapEncapCond(final List<EncapsulateConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G28, r.className(),
                        "Complex condition (depth %d) should be extracted".formatted(r.depth())))
                .toList();
    }

    private List<Finding> mapNullDensity(final List<NullDensityRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_2, r.className(),
                        "Method '%s' has %d null checks".formatted(r.methodName(), r.nullCheckCount())))
                .toList();
    }

    private List<Finding> mapClassLength(final List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapLargeRecord(final List<LargeRecordRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(), r.lineNumber(),
                        "Record '%s' has %d components — too many constructor parameters"
                                .formatted(r.className(), r.componentCount())))
                .toList();
    }

    private List<Finding> mapDisabledTest(final List<DisabledTestRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.T3, r.className() + ".java", -1, -1,
                        "@%s on '%s' without meaningful reason".formatted(r.annotation(), r.methodName()),
                        Severity.INFO, Confidence.HIGH, TOOL, "DisabledTestRecipe"))
                .toList();
    }

    private List<Finding> mapSwitchOnType(final List<SwitchOnTypeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        "Type switch in '%s': %s".formatted(r.methodName(), r.pattern())))
                .toList();
    }

    private List<Finding> mapCommentedCode(final List<CommentedCodeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.C5, r.sourceFile(), r.lineNumber(), r.lineNumber(),
                        "Commented-out code: %s".formatted(r.commentPreview()),
                        Severity.WARNING, Confidence.MEDIUM, TOOL, "CommentedCodeRecipe"))
                .toList();
    }

    private List<Finding> mapMumblingComment(final List<MumblingCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.C3, r.className(), r.lineNumber(),
                        "Mumbling comment in '%s': %s".formatted(r.methodName(), r.commentPreview())))
                .toList();
    }

    private List<Finding> mapSectionComment(final List<SectionCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G34, r.className(), r.lineNumber(),
                        "Method '%s' has %d section comments".formatted(r.methodName(), r.sectionCount())))
                .toList();
    }

    private List<Finding> mapEncodingNaming(final List<EncodingNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N6, r.className(), r.lineNumber(),
                        "%s '%s' uses %s".formatted(r.elementKind(), r.elementName(), r.violationType())))
                .toList();
    }

    private List<Finding> mapVerticalSeparation(final List<VerticalSeparationRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G10, r.className(), r.declarationLine(),
                        ("'%s' is declared in %s() but not used until %d lines later"
                                + " — move the declaration closer to line %d")
                                .formatted(r.variableName(), r.methodName(), r.distance(), r.firstUseLine())))
                .toList();
    }

    private List<Finding> mapInheritConstants(final List<InheritConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.J2, r.className(), r.lineNumber(),
                        "Class inherits constants from interface '%s'".formatted(r.interfaceName())))
                .toList();
    }

    private List<Finding> mapEnumForConstants(final List<EnumForConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.J3, r.className(), r.lineNumber(),
                        "%d static final fields with prefix '%s' should be an enum"
                                .formatted(r.fieldCount(), r.prefix())))
                .toList();
    }

    private List<Finding> mapShortNames(final List<ShortVariableNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N5, r.className(), r.lineNumber(),
                        "'%s' in %s() is not a meaningful name — rename to reveal intent (%s)"
                                .formatted(r.variableName(), r.methodName(), r.context())))
                .toList();
    }

    private List<Finding> mapMagicStrings(final List<MagicStringRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G25, r.className(), r.lineNumber(),
                        "String \"%s\" appears %d times — extract to a named constant"
                                .formatted(r.value(), r.count())))
                .toList();
    }

    private List<Finding> mapWhitespaceSplit(final List<WhitespaceSplitMethodRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        ("Method '%s' has %d blank-line sections across %d lines"
                                + " — each section should be its own method")
                                .formatted(r.methodName(), r.blankLineCount(), r.totalLines())))
                .toList();
    }

    private List<Finding> mapPrivateMethod(final List<PrivateMethodTestabilityRecipe.PrivateMethodTestabilityRow> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.T1, r.className(), r.lineNumber(),
                        ("Private method '%s' (%d lines) should be package-private"
                                + " so it can be tested directly")
                                .formatted(r.methodName(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapStringSwitch(final List<StringSwitchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        ("Switch on String '%s' with %d cases"
                                + " — replace with an enum that encapsulates the behaviour")
                                .formatted(r.selectorName(), r.caseCount())))
                .toList();
    }

    private List<Finding> mapVisibility(final List<VisibilityReductionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G8, r.className(), r.lineNumber(),
                        "Field '%s' is %s and mutable — should be private"
                                .formatted(r.fieldName(), r.currentVisibility())))
                .toList();
    }

    private List<Finding> mapImperativeLoop(final List<ImperativeLoopRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        "Loop in '%s' (%s) can be replaced with a stream operation"
                                .formatted(r.methodName(), r.loopPattern())))
                .toList();
    }

    private List<Finding> mapUncheckedCast(final List<UncheckedCastRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G4, r.className(), r.lineNumber(),
                        "@SuppressWarnings(\"unchecked\") on '%s' — redesign to avoid unsafe casts"
                                .formatted(r.memberName())))
                .toList();
    }

    private List<Finding> mapFeatureEnvy(final List<FeatureEnvyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G14, r.className(), r.lineNumber(),
                        ("Method '%s' calls %d methods on '%s' but only %d on its own class"
                                + " — it wants to live elsewhere")
                                .formatted(r.methodName(), r.externalCallCount(),
                                        r.enviedClass(), r.selfCallCount())))
                .toList();
    }

    private List<Finding> mapNestedTernary(final List<NestedTernaryRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G16, r.className(), r.lineNumber(),
                        "Ternary nested %d deep in '%s' — extract to an if/else or a named method"
                                .formatted(r.depth(), r.methodName())))
                .toList();
    }

    private List<Finding> mapMissingExplanatory(final List<MissingExplanatoryVariableRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G19, r.className(), r.lineNumber(),
                        "Complex expression in '%s' should be extracted to a named variable: %s"
                                .formatted(r.methodName(), r.expressionPreview())))
                .toList();
    }

    private List<Finding> mapBoundaryCondition(final List<BoundaryConditionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G33, r.className(), r.lineNumber(),
                        "Boundary adjustment '%s' in '%s' — extract to a named variable"
                                .formatted(r.expression(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSideEffectNaming(final List<SideEffectNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N7, r.className(), r.lineNumber(),
                        "Method '%s' is named like a query but %s — rename to reveal the side effect"
                                .formatted(r.methodName(), r.sideEffect())))
                .toList();
    }

    private List<Finding> mapInconsistentNaming(final List<InconsistentNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G11, r.className(), r.lineNumber(),
                        "Class uses inconsistent prefixes %s for the same concept: %s"
                                .formatted(r.conflictingPrefixes(), r.methodNames())))
                .toList();
    }

    private List<Finding> mapBadClassName(final List<BadClassNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N1, r.className(),
                        "Class '%s' uses bad suffix '%s' — name after what it represents, not its role"
                                .formatted(r.className(), r.suffix())))
                .toList();
    }

    private List<Finding> mapSystemOut(final List<SystemOutRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G4, r.className(),
                        "'%s' bypasses structured logging — use @Slf4j instead".formatted(r.call())))
                .toList();
    }

    private List<Finding> mapLegacyFileApi(final List<LegacyFileApiRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G26, r.className(),
                        "'%s' is a legacy API — use java.nio.file.Path and Files instead"
                                .formatted(r.legacyType())))
                .toList();
    }

    private List<Finding> mapMultipleAssert(final List<MultipleAssertRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.T1, r.className(),
                        "Test '%s' has %d consecutive assertions — wrap in assertAll"
                                .formatted(r.methodName(), r.assertCount())))
                .toList();
    }

    private List<Finding> mapLargeConstructor(final List<LargeConstructorRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(),
                        "Constructor has %d parameters — introduce a parameter object or builder"
                                .formatted(r.parameterCount())))
                .toList();
    }

    private List<Finding> mapInappropriateStatic(final List<InappropriateStaticRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G18, r.className(),
                        "Method '%s' does not use instance state — consider making it static or extracting"
                                .formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapStringlyTypedDispatch(final List<StringlyTypedDispatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G23, r.className(),
                        ("Method '%s' dispatches on String parameter '%s' with %d branches"
                                + " — use an enum or split into separate methods")
                                .formatted(r.methodName(), r.parameterName(), r.branchCount())))
                .toList();
    }

    private List<Finding> mapConfigurableData(final List<ConfigurableDataRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G35, r.className(),
                        "Magic number %s in private method '%s' — extract to a named constant"
                                .formatted(r.literalValue(), r.methodName())))
                .toList();
    }

    private List<Finding> mapEmbeddedLanguage(final List<EmbeddedLanguageRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G1, r.className(),
                        "Embedded %s in method '%s' — extract to a template or resource file"
                                .formatted(r.language().toUpperCase(Locale.ROOT), r.methodName())))
                .toList();
    }

    private List<Finding> mapGuardClause(final List<GuardClauseRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(),
                        "Method '%s' has %d guard clauses — simplify with early return or extract filter"
                                .formatted(r.methodName(), r.guardCount())))
                .toList();
    }

    private List<Finding> mapBaseClassDependency(final List<BaseClassDependencyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G7, r.className(),
                        "'%s' depends on its derivative '%s' — invert the dependency"
                                .formatted(r.className(), r.derivativeName())))
                .toList();
    }

    private List<Finding> mapArtificialCoupling(final List<ArtificialCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G13, r.declaringClass(),
                        "Constant '%s' defined in '%s' but only used in '%s' — move it"
                                .formatted(r.constantName(), r.declaringClass(), r.usedInClass())))
                .toList();
    }

    private List<Finding> mapHardcodedList(final List<HardcodedListRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G35, r.className(),
                        "Field '%s' has %d hardcoded literals — load from configuration"
                                .formatted(r.fieldName(), r.literalCount())))
                .toList();
    }

    private List<Finding> mapSelectorArgument(final List<SelectorArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G15, r.className(),
                        ("Method '%s' uses %s parameter '%s' to select behaviour"
                                + " — split into separate methods")
                                .formatted(r.methodName(), r.parameterType(), r.parameterName())))
                .toList();
    }

    private List<Finding> mapObsoleteComment(final List<ObsoleteCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.C2, r.className(),
                        "Comment references '%s' which is not in scope — update or remove"
                                .formatted(r.missingIdentifier())))
                .toList();
    }

    private List<Finding> mapTemporalCoupling(final List<TemporalCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G31, r.className(),
                        ("Method '%s' has %d consecutive void calls with no data dependency"
                                + " — make the order explicit")
                                .formatted(r.methodName(), r.callCount())))
                .toList();
    }

    private List<Finding> mapBroadCatch(final List<BroadCatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_1, r.className(),
                        "Method '%s' catches %s — catch specific exception types instead"
                                .formatted(r.methodName(), r.caughtType())))
                .toList();
    }

    private List<Finding> mapRawGeneric(final List<RawGenericRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G26, r.className(),
                        "'%s' in '%s' uses Object type parameter — use a typed record or specific generic"
                                .formatted(r.typeName(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSwallowedException(final List<SwallowedExceptionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G4, r.className(),
                        "Method '%s' catches %s and silently swallows it — handle or propagate"
                                .formatted(r.methodName(), r.exceptionType())))
                .toList();
    }

    private List<Finding> mapInconsistentReturn(final List<InconsistentReturnRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F2, r.className(),
                        ("Class has %d methods returning collections and %d void methods"
                                + " mutating collection params — pick one style")
                                .formatted(r.returningMethods(), r.mutatingMethods())))
                .toList();
    }

    private List<Finding> mapSuppressedWarning(final List<SuppressedWarningRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G4, r.className(),
                        "@SuppressWarnings(\"%s\") on '%s' — redesign to avoid unsafe operations"
                                .formatted(r.warningType(), r.methodName())))
                .toList();
    }

    private Finding finding(final HeuristicCode code, final String className, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, -1, -1,
                message, OpenRewriteFindingSource.severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private Finding finding(final HeuristicCode code, final String className,
                            final int line, final String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, line, line,
                message, OpenRewriteFindingSource.severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private String resolveSourcePath(final String className) {
        return classNameToSourcePath.getOrDefault(className, className + ".java");
    }

    private static Map<String, String> buildSourcePathIndex(final List<SourceFile> parsed) {
        final Map<String, String> index = new HashMap<>();
        parsed.forEach(sf -> {
            final String path = sf.getSourcePath().toString();
            final String fileName = path.contains("/")
                    ? path.substring(path.lastIndexOf('/') + 1)
                    : path;
            final String className = fileName.endsWith(".java")
                    ? fileName.substring(0, fileName.length() - 5)
                    : fileName;
            index.put(className, path);
        });
        return index;
    }
}
