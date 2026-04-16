package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.fiftieshousewife.cleancode.recipes.*;
import org.openrewrite.ScanningRecipe;

import java.util.List;

final class OpenRewriteCommentNamingMapper {

    private final OpenRewriteFindingFactory findings;

    OpenRewriteCommentNamingMapper(final OpenRewriteFindingFactory findings) {
        this.findings = findings;
    }

    List<Finding> map(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
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
            case SideEffectNamingRecipe r -> mapSideEffectNaming(r.collectedRows());
            case InconsistentNamingRecipe r -> mapInconsistentNaming(r.collectedRows());
            case BadClassNameRecipe r -> mapBadClassName(r.collectedRows());
            case ObsoleteCommentRecipe r -> mapObsoleteComment(r.collectedRows());
            case MissingExplanatoryVariableRecipe r -> mapMissingExplanatory(r.collectedRows());
            case BoundaryConditionRecipe r -> mapBoundaryCondition(r.collectedRows());
            default -> List.of();
        };
    }

    private List<Finding> mapCommentedCode(final List<CommentedCodeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.C5, r.sourceFile(), r.lineNumber(), r.lineNumber(),
                        "Commented-out code: %s".formatted(r.commentPreview()),
                        Severity.WARNING, Confidence.MEDIUM, OpenRewriteFindingFactory.TOOL, "CommentedCodeRecipe"))
                .toList();
    }

    private List<Finding> mapMumblingComment(final List<MumblingCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.C3, r.className(), r.lineNumber(),
                        "Mumbling comment in '%s': %s".formatted(r.methodName(), r.commentPreview())))
                .toList();
    }

    private List<Finding> mapSectionComment(final List<SectionCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G34, r.className(), r.lineNumber(),
                        "Method '%s' has %d section comments".formatted(r.methodName(), r.sectionCount())))
                .toList();
    }

    private List<Finding> mapEncodingNaming(final List<EncodingNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.N6, r.className(), r.lineNumber(),
                        "%s '%s' uses %s".formatted(r.elementKind(), r.elementName(), r.violationType())))
                .toList();
    }

    private List<Finding> mapVerticalSeparation(final List<VerticalSeparationRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G10, r.className(), r.declarationLine(),
                        verticalSeparationMessage(r)))
                .toList();
    }

    private static String verticalSeparationMessage(final VerticalSeparationRecipe.Row r) {
        return ("'%s' is declared in %s() but not used until %d lines later"
                + " — move the declaration closer to line %d")
                .formatted(r.variableName(), r.methodName(), r.distance(), r.firstUseLine());
    }

    private List<Finding> mapInheritConstants(final List<InheritConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.J2, r.className(), r.lineNumber(),
                        "Class inherits constants from interface '%s'".formatted(r.interfaceName())))
                .toList();
    }

    private List<Finding> mapEnumForConstants(final List<EnumForConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.J3, r.className(), r.lineNumber(),
                        "%d static final fields with prefix '%s' should be an enum"
                                .formatted(r.fieldCount(), r.prefix())))
                .toList();
    }

    private List<Finding> mapShortNames(final List<ShortVariableNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.N5, r.className(), r.lineNumber(),
                        shortNameMessage(r.variableName(), r.methodName(), r.context())))
                .toList();
    }

    private static String shortNameMessage(final String variableName, final String methodName, final String context) {
        return "'%s' in %s() is not a meaningful name — rename to reveal intent (%s)"
                .formatted(variableName, methodName, context);
    }

    private List<Finding> mapMagicStrings(final List<MagicStringRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G25, r.className(), r.lineNumber(),
                        "String \"%s\" appears %d times — extract to a named constant"
                                .formatted(r.value(), r.count())))
                .toList();
    }

    private List<Finding> mapWhitespaceSplit(final List<WhitespaceSplitMethodRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        whitespaceSplitMessage(r)))
                .toList();
    }

    private static String whitespaceSplitMessage(final WhitespaceSplitMethodRecipe.Row r) {
        return ("Method '%s' has %d blank-line sections across %d lines"
                + " — each section should be its own method")
                .formatted(r.methodName(), r.blankLineCount(), r.totalLines());
    }

    private List<Finding> mapPrivateMethod(
            final List<PrivateMethodTestabilityRecipe.PrivateMethodTestabilityRow> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.T1, r.className(), r.lineNumber(),
                        privateMethodMessage(r.methodName(), r.lineCount())))
                .toList();
    }

    private static String privateMethodMessage(final String methodName, final int lineCount) {
        return "Private method '%s' (%d lines) should be package-private so it can be tested directly"
                .formatted(methodName, lineCount);
    }

    private List<Finding> mapStringSwitch(final List<StringSwitchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        stringSwitchMessage(r.selectorName(), r.caseCount())))
                .toList();
    }

    private static String stringSwitchMessage(final String selectorName, final int caseCount) {
        return "Switch on String '%s' with %d cases — replace with an enum that encapsulates the behaviour"
                .formatted(selectorName, caseCount);
    }

    private List<Finding> mapSideEffectNaming(final List<SideEffectNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.N7, r.className(), r.lineNumber(),
                        "Method '%s' is named like a query but %s — rename to reveal the side effect"
                                .formatted(r.methodName(), r.sideEffect())))
                .toList();
    }

    private List<Finding> mapInconsistentNaming(final List<InconsistentNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G11, r.className(), r.lineNumber(),
                        "Class uses inconsistent prefixes %s for the same concept: %s"
                                .formatted(r.conflictingPrefixes(), r.methodNames())))
                .toList();
    }

    private List<Finding> mapBadClassName(final List<BadClassNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.N1, r.className(),
                        badClassNameMessage(r.className(), r.suffix())))
                .toList();
    }

    private static String badClassNameMessage(final String className, final String suffix) {
        return "Class '%s' uses bad suffix '%s' — name after what it represents, not its role"
                .formatted(className, suffix);
    }

    private List<Finding> mapObsoleteComment(final List<ObsoleteCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.C2, r.className(),
                        "Comment references '%s' which is not in scope — update or remove"
                                .formatted(r.missingIdentifier())))
                .toList();
    }

    private List<Finding> mapMissingExplanatory(final List<MissingExplanatoryVariableRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G19, r.className(), r.lineNumber(),
                        "Complex expression in '%s' should be extracted to a named variable: %s"
                                .formatted(r.methodName(), r.expressionPreview())))
                .toList();
    }

    private List<Finding> mapBoundaryCondition(final List<BoundaryConditionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findings.finding(HeuristicCode.G33, r.className(), r.lineNumber(),
                        "Boundary adjustment '%s' in '%s' — extract to a named variable"
                                .formatted(r.expression(), r.methodName())))
                .toList();
    }
}
