package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.recipes.*;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class OpenRewriteFindingSource implements FindingSource {

    private static final String TOOL = "openrewrite";

    private final RecipeThresholds thresholds;

    public OpenRewriteFindingSource() {
        this(RecipeThresholds.defaults());
    }

    public OpenRewriteFindingSource(final RecipeThresholds thresholds) {
        this.thresholds = thresholds;
    }

    private static final Set<HeuristicCode> COVERED = Set.of(
            HeuristicCode.F2, HeuristicCode.F3,
            HeuristicCode.C3, HeuristicCode.C5,
            HeuristicCode.Ch3_1,
            HeuristicCode.Ch7_1, HeuristicCode.Ch7_2,
            HeuristicCode.Ch10_1, HeuristicCode.Ch10_2,
            HeuristicCode.G4, HeuristicCode.G8,
            HeuristicCode.G10, HeuristicCode.G11, HeuristicCode.G14,
            HeuristicCode.G16, HeuristicCode.G19,
            HeuristicCode.G23, HeuristicCode.G25,
            HeuristicCode.G28, HeuristicCode.G29,
            HeuristicCode.G30, HeuristicCode.G33, HeuristicCode.G34, HeuristicCode.G36,
            HeuristicCode.J2, HeuristicCode.J3,
            HeuristicCode.N5, HeuristicCode.N6, HeuristicCode.N7,
            HeuristicCode.T3, HeuristicCode.T4);

    @Override
    public String id() {
        return TOOL;
    }

    @Override
    public String displayName() {
        return "OpenRewrite";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return COVERED;
    }

    private Map<String, String> classNameToSourcePath = Map.of();

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final List<Path> javaFiles = collectSourceFiles(context);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        final List<SourceFile> parsed = parseSourceFiles(javaFiles);
        classNameToSourcePath = buildSourcePathIndex(parsed);
        final List<ScanningRecipe<?>> recipes = createRecipes();
        recipes.forEach(recipe -> runRecipe(parsed, recipe));
        return extractFindings(recipes);
    }

    private Map<String, String> buildSourcePathIndex(List<SourceFile> parsed) {
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

    private List<ScanningRecipe<?>> createRecipes() {
        return List.of(
                new FlagArgumentRecipe(),
                new OutputArgumentRecipe(),
                new CatchLogContinueRecipe(),
                new NegativeConditionalRecipe(),
                new LawOfDemeterRecipe(thresholds.chainDepthThreshold()),
                new EncapsulateConditionalRecipe(),
                new NullDensityRecipe(thresholds.nullCheckDensity()),
                new ClassLineLengthRecipe(thresholds.classLineCount()),
                new LargeRecordRecipe(thresholds.recordComponentCount()),
                new DisabledTestRecipe(),
                new SwitchOnTypeRecipe(),
                new CommentedCodeRecipe(),
                new MumblingCommentRecipe(),
                new SectionCommentRecipe(),
                new EncodingNamingRecipe(),
                new VerticalSeparationRecipe(thresholds.verticalSeparationDistance()),
                new InheritConstantsRecipe(),
                new EnumForConstantsRecipe(),
                new ShortVariableNameRecipe(thresholds.shortNameMinLength()),
                new MagicStringRecipe(thresholds.magicStringMinOccurrences()),
                new WhitespaceSplitMethodRecipe(thresholds.methodBlankLineSections()),
                new PrivateMethodTestabilityRecipe(thresholds.privateMethodMinLines()),
                new StringSwitchRecipe(thresholds.stringSwitchMinCases()),
                new VisibilityReductionRecipe(),
                new ImperativeLoopRecipe(),
                new UncheckedCastRecipe(),
                new FeatureEnvyRecipe(),
                new NestedTernaryRecipe(),
                new MissingExplanatoryVariableRecipe(),
                new BoundaryConditionRecipe(),
                new SideEffectNamingRecipe(),
                new InconsistentNamingRecipe());
    }

    @SuppressWarnings("unchecked")
    private List<Finding> extractFindings(List<ScanningRecipe<?>> recipes) {
        final List<Finding> findings = new ArrayList<>();
        recipes.forEach(recipe -> findings.addAll(mapRecipe(recipe)));
        return findings;
    }

    private List<Finding> mapRecipe(ScanningRecipe<?> recipe) {
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
            default -> List.of();
        };
    }

    private List<Finding> mapFlagArgs(List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F3, r.className(),
                        "Method '%s' takes boolean parameter '%s' — split into two methods instead".formatted(r.methodName(), r.paramName())))
                .toList();
    }

    private List<Finding> mapOutputArgs(List<OutputArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F2, r.className(), r.lineNumber(),
                        "Method '%s' mutates its argument '%s' (%s) — return the result instead".formatted(
                                r.methodName(), r.paramName(), r.paramType())))
                .toList();
    }

    private List<Finding> mapCatchLog(List<CatchLogContinueRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_1, r.className(),
                        "Catch block in '%s' only logs or is empty".formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapNegCond(List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(),
                        "Double negation: %s".formatted(r.expression())))
                .toList();
    }

    private List<Finding> mapDemeter(List<LawOfDemeterRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G36, r.className(),
                        "Method chain depth %d: %s".formatted(r.depth(), r.chain())))
                .toList();
    }

    private List<Finding> mapEncapCond(List<EncapsulateConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G28, r.className(),
                        "Complex condition (depth %d) should be extracted".formatted(r.depth())))
                .toList();
    }

    private List<Finding> mapNullDensity(List<NullDensityRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_2, r.className(),
                        "Method '%s' has %d null checks".formatted(r.methodName(), r.nullCheckCount())))
                .toList();
    }

    private List<Finding> mapClassLength(List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapLargeRecord(List<LargeRecordRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_2, r.className(), r.lineNumber(),
                        "Record '%s' has %d components".formatted(r.className(), r.componentCount())))
                .toList();
    }

    private List<Finding> mapDisabledTest(List<DisabledTestRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.T3, r.className() + ".java", -1, -1,
                        "@%s on '%s' without meaningful reason".formatted(r.annotation(), r.methodName()),
                        Severity.INFO, Confidence.HIGH, TOOL, "DisabledTestRecipe"))
                .toList();
    }

    private List<Finding> mapSwitchOnType(List<SwitchOnTypeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        "Type switch in '%s': %s".formatted(r.methodName(), r.pattern())))
                .toList();
    }

    private List<Finding> mapCommentedCode(List<CommentedCodeRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.C5, r.sourceFile(), r.lineNumber(), r.lineNumber(),
                        "Commented-out code: %s".formatted(r.commentPreview()),
                        Severity.WARNING, Confidence.MEDIUM, TOOL, "CommentedCodeRecipe"))
                .toList();
    }

    private List<Finding> mapMumblingComment(List<MumblingCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.C3, r.className(), r.lineNumber(),
                        "Mumbling comment in '%s': %s".formatted(r.methodName(), r.commentPreview())))
                .toList();
    }

    private List<Finding> mapSectionComment(List<SectionCommentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G34, r.className(), r.lineNumber(),
                        "Method '%s' has %d section comments".formatted(r.methodName(), r.sectionCount())))
                .toList();
    }

    private List<Finding> mapEncodingNaming(List<EncodingNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N6, r.className(), r.lineNumber(),
                        "%s '%s' uses %s".formatted(r.elementKind(), r.elementName(), r.violationType())))
                .toList();
    }

    private List<Finding> mapVerticalSeparation(List<VerticalSeparationRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G10, r.className(), r.declarationLine(),
                        "'%s' is declared in %s() but not used until %d lines later — move the declaration closer to line %d".formatted(
                                r.variableName(), r.methodName(), r.distance(), r.firstUseLine())))
                .toList();
    }

    private List<Finding> mapInheritConstants(List<InheritConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.J2, r.className(), r.lineNumber(),
                        "Class inherits constants from interface '%s'".formatted(r.interfaceName())))
                .toList();
    }

    private List<Finding> mapEnumForConstants(List<EnumForConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.J3, r.className(), r.lineNumber(),
                        "%d static final fields with prefix '%s' should be an enum".formatted(
                                r.fieldCount(), r.prefix())))
                .toList();
    }

    private List<Finding> mapShortNames(List<ShortVariableNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N5, r.className(), r.lineNumber(),
                        "'%s' in %s() is not a meaningful name — rename to reveal intent (%s)".formatted(
                                r.variableName(), r.methodName(), r.context())))
                .toList();
    }

    private List<Finding> mapMagicStrings(List<MagicStringRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G25, r.className(), r.lineNumber(),
                        "String \"%s\" appears %d times — extract to a named constant".formatted(
                                r.value(), r.count())))
                .toList();
    }

    private List<Finding> mapWhitespaceSplit(List<WhitespaceSplitMethodRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        "Method '%s' has %d blank-line sections across %d lines — each section should be its own method".formatted(
                                r.methodName(), r.blankLineCount(), r.totalLines())))
                .toList();
    }

    private List<Finding> mapPrivateMethod(List<PrivateMethodTestabilityRecipe.PrivateMethodTestabilityRow> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch3_1, r.className(), r.lineNumber(),
                        "Private method '%s' (%d lines) should be package-private so it can be tested directly".formatted(
                                r.methodName(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapStringSwitch(List<StringSwitchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G23, r.className(), r.lineNumber(),
                        "Switch on String '%s' with %d cases — replace with an enum that encapsulates the behaviour".formatted(
                                r.selectorName(), r.caseCount())))
                .toList();
    }

    private List<Finding> mapVisibility(List<VisibilityReductionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G8, r.className(), r.lineNumber(),
                        "Field '%s' is %s and mutable — should be private".formatted(
                                r.fieldName(), r.currentVisibility())))
                .toList();
    }

    private List<Finding> mapImperativeLoop(List<ImperativeLoopRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G30, r.className(), r.lineNumber(),
                        "Loop in '%s' (%s) can be replaced with a stream operation".formatted(
                                r.methodName(), r.loopPattern())))
                .toList();
    }

    private List<Finding> mapUncheckedCast(List<UncheckedCastRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G4, r.className(), r.lineNumber(),
                        "@SuppressWarnings(\"unchecked\") on '%s' — redesign to avoid unsafe casts".formatted(
                                r.memberName())))
                .toList();
    }

    private List<Finding> mapFeatureEnvy(List<FeatureEnvyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G14, r.className(), r.lineNumber(),
                        "Method '%s' calls %d methods on '%s' but only %d on its own class — it wants to live elsewhere".formatted(
                                r.methodName(), r.externalCallCount(), r.enviedClass(), r.selfCallCount())))
                .toList();
    }

    private List<Finding> mapNestedTernary(List<NestedTernaryRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G16, r.className(), r.lineNumber(),
                        "Ternary nested %d deep in '%s' — extract to an if/else or a named method".formatted(
                                r.depth(), r.methodName())))
                .toList();
    }

    private List<Finding> mapMissingExplanatory(List<MissingExplanatoryVariableRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G19, r.className(), r.lineNumber(),
                        "Complex expression in '%s' should be extracted to a named variable: %s".formatted(
                                r.methodName(), r.expressionPreview())))
                .toList();
    }

    private List<Finding> mapBoundaryCondition(List<BoundaryConditionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G33, r.className(), r.lineNumber(),
                        "Boundary adjustment '%s' in '%s' — extract to a named variable".formatted(
                                r.expression(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSideEffectNaming(List<SideEffectNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N7, r.className(), r.lineNumber(),
                        "Method '%s' is named like a query but %s — rename to reveal the side effect".formatted(
                                r.methodName(), r.sideEffect())))
                .toList();
    }

    private List<Finding> mapInconsistentNaming(List<InconsistentNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G11, r.className(), r.lineNumber(),
                        "Class uses inconsistent prefixes %s for the same concept: %s".formatted(
                                r.conflictingPrefixes(), r.methodNames())))
                .toList();
    }

    private List<Path> collectSourceFiles(ProjectContext context) {
        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> {
                    try (Stream<Path> walk = Files.walk(root)) {
                        walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
                    } catch (IOException ignored) {
                    }
                });
        return files;
    }

    private List<SourceFile> parseSourceFiles(List<Path> files) {
        return JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(files, null, new InMemoryExecutionContext(Throwable::printStackTrace))
                .toList();
    }

    private void runRecipe(List<SourceFile> parsed, ScanningRecipe<?> recipe) {
        final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
    }

    private Finding finding(HeuristicCode code, String className, String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, -1, -1,
                message, Severity.WARNING, Confidence.HIGH, TOOL, code.name());
    }

    private Finding finding(HeuristicCode code, String className, int line, String message) {
        final String sourcePath = resolveSourcePath(className);
        return Finding.at(code, sourcePath, line, line,
                message, Severity.WARNING, Confidence.HIGH, TOOL, code.name());
    }

    private String resolveSourcePath(String className) {
        return classNameToSourcePath.getOrDefault(className, className + ".java");
    }
}
