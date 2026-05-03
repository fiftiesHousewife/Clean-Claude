package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.recipes.*;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;

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

    private static final Map<HeuristicCode, Severity> DEFAULT_SEVERITY = Map.of(
            HeuristicCode.G4, Severity.ERROR,
            HeuristicCode.Ch7_1, Severity.ERROR,
            HeuristicCode.F2, Severity.ERROR,
            HeuristicCode.G8, Severity.ERROR);

    static Severity severityFor(final HeuristicCode code) {
        return DEFAULT_SEVERITY.getOrDefault(code, Severity.WARNING);
    }

    private static final Set<HeuristicCode> COVERED = Set.of(
            HeuristicCode.F1, HeuristicCode.F2, HeuristicCode.F3,
            HeuristicCode.C3, HeuristicCode.C5,
            HeuristicCode.Ch3_1,
            HeuristicCode.Ch7_1, HeuristicCode.Ch7_2,
            HeuristicCode.Ch10_1, HeuristicCode.Ch10_2,
            HeuristicCode.G4, HeuristicCode.G8,
            HeuristicCode.G10, HeuristicCode.G11, HeuristicCode.G14,
            HeuristicCode.G16, HeuristicCode.G19,
            HeuristicCode.G23, HeuristicCode.G24, HeuristicCode.G25, HeuristicCode.G26,
            HeuristicCode.G28, HeuristicCode.G29,
            HeuristicCode.G30, HeuristicCode.G33, HeuristicCode.G34, HeuristicCode.G36,
            HeuristicCode.J2, HeuristicCode.J3,
            HeuristicCode.N1, HeuristicCode.N5, HeuristicCode.N6, HeuristicCode.N7,
            HeuristicCode.T1, HeuristicCode.T3, HeuristicCode.T4);

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
    private Map<String, J.CompilationUnit> classNameToCompilationUnit = Map.of();

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final List<Path> javaFiles = collectSourceFiles(context);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        final List<SourceFile> parsed = parseSourceFiles(javaFiles);
        classNameToSourcePath = buildSourcePathIndex(parsed);
        classNameToCompilationUnit = buildCompilationUnitIndex(parsed);
        final List<ScanningRecipe<?>> recipes = createRecipes();
        recipes.forEach(recipe -> runRecipe(parsed, recipe));
        return extractFindings(recipes);
    }

    private Map<String, J.CompilationUnit> buildCompilationUnitIndex(final List<SourceFile> parsed) {
        final Map<String, J.CompilationUnit> index = new HashMap<>();
        parsed.forEach(sf -> {
            if (!(sf instanceof J.CompilationUnit cu)) {
                return;
            }
            cu.getClasses().forEach(c -> index.put(c.getSimpleName(), cu));
        });
        return index;
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
                new SectionCommentRecipe(thresholds.sectionCommentThreshold()),
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
                new InconsistentNamingRecipe(),
                new BadClassNameRecipe(),
                new SystemOutRecipe(),
                new FixedStringLogRecipe(),
                new LegacyTypesRecipe(),
                new MultipleAssertRecipe(),
                new LargeConstructorRecipe(thresholds.recordComponentCount()),
                new InappropriateStaticRecipe(),
                new StringlyTypedDispatchRecipe(),
                new ConfigurableDataRecipe(thresholds.magicNumberMinValue()),
                new EmbeddedLanguageRecipe(),
                new GuardClauseRecipe(),
                new BaseClassDependencyRecipe(),
                new ArtificialCouplingRecipe(),
                new HardcodedListRecipe(thresholds.hardcodedListMinLiterals()),
                new SelectorArgumentRecipe(),
                new ObsoleteCommentRecipe(),
                new TemporalCouplingRecipe(thresholds.temporalCouplingMinCalls()),
                new BroadCatchRecipe(),
                new RawGenericRecipe(),
                new SwallowedExceptionRecipe(),
                new InconsistentReturnRecipe(),
                new SuppressedWarningRecipe(),
                new FullyQualifiedReferenceRecipe(),
                new StringBuilderThreadingRecipe());
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
            case BadClassNameRecipe r -> mapBadClassName(r.collectedRows());
            case SystemOutRecipe r -> mapSystemOut(r.collectedRows());
            case FixedStringLogRecipe r -> mapFixedStringLog(r.collectedRows());
            case LegacyTypesRecipe r -> mapLegacyTypes(r.collectedRows());
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
            case StringBuilderThreadingRecipe r -> mapStringBuilderThreading(r.collectedRows());
            default -> List.of();
        };
    }

    private List<Finding> mapStringBuilderThreading(List<StringBuilderThreadingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> switch (r.kind()) {
                    case NAMING -> findingForMethod(HeuristicCode.G24, r.className(), r.methodName(),
                            "Local StringBuilder named '%s' in '%s' — name it after what it builds (html, markdown, buffer)"
                                    .formatted(r.variableName(), r.methodName()));
                    case THREADING -> findingForMethod(HeuristicCode.F2, r.className(), r.methodName(),
                            "Method '%s' mutates StringBuilder parameter '%s' via .append() — return the string instead"
                                    .formatted(r.methodName(), r.variableName()));
                })
                .toList();
    }

    private List<Finding> mapFullyQualifiedReferences(List<FullyQualifiedReferenceRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.G12, r.sourceFile(), 0, 0,
                        "%d inline fully-qualified type reference(s); first: %s"
                                .formatted(r.count(), r.samplePreview()),
                        Severity.WARNING, Confidence.HIGH, TOOL, "FullyQualifiedReferenceRecipe"))
                .toList();
    }

    private List<Finding> mapFlagArgs(List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.F3, r.className(), r.methodName(),
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
                .map(r -> {
                    final String sourcePath = resolveSourcePath(r.className());
                    final int line = lineOfCatchInMethod(r.className(), r.methodName(), r.exceptionType());
                    final String message = "Catch block in '%s' only logs or is empty".formatted(r.methodName());
                    return Finding.at(HeuristicCode.Ch7_1, sourcePath, line, line,
                            message, severityFor(HeuristicCode.Ch7_1), Confidence.HIGH, TOOL,
                            HeuristicCode.Ch7_1.name());
                })
                .toList();
    }

    private List<Finding> mapNegCond(List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(), r.lineNumber(),
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
        // The recipe records lineNumber=-1, so resolve via source-text:
        // walk the method's body for the first multi-operator `if` line
        // (depth-2 conditional means at least one logical operator).
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFirstComplexIf(r.className(), r.methodName());
                    return finding(HeuristicCode.G28, r.className(), line,
                            "Complex condition (depth %d) should be extracted".formatted(r.depth()));
                })
                .toList();
    }

    /**
     * Returns the source line of the first {@code if (} inside the named
     * method whose condition contains a logical operator ({@code &&} or
     * {@code ||}). Falls back to the method declaration line if no such
     * if is found.
     */
    private int lineOfFirstComplexIf(final String className, final String methodName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1
                    && line.contains("if (")
                    && (line.contains("&&") || line.contains("||"))) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return methodLine;
                    }
                }
            }
        }
        return methodLine;
    }

    private List<Finding> mapNullDensity(List<NullDensityRecipe.Row> rows) {
        // Per-method null-density: anchor at the first `return null` or
        // `== null` line inside the method so the snippet shows the
        // specific null-handling, not just the method header.
        return rows.stream()
                .map(r -> {
                    final String sourcePath = resolveSourcePath(r.className());
                    final int line = lineOfFirstNullCheckInMethod(r.className(), r.methodName());
                    final String message = "Method '%s' has %d null checks"
                            .formatted(r.methodName(), r.nullCheckCount());
                    return Finding.at(HeuristicCode.Ch7_2, sourcePath, line, line,
                            message, severityFor(HeuristicCode.Ch7_2), Confidence.HIGH, TOOL,
                            HeuristicCode.Ch7_2.name());
                })
                .toList();
    }

    /**
     * First {@code return null} or {@code == null} / {@code != null}
     * appearance inside the named method's body. Falls back to the
     * method declaration line when no match is found.
     */
    private int lineOfFirstNullCheckInMethod(final String className, final String methodName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return lineOfMethod(className, methodName);
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return lineOfMethod(className, methodName);
        }
        final java.util.regex.Pattern nullCheck = java.util.regex.Pattern.compile(
                "\\breturn\\s+null\\b|==\\s*null\\b|!=\\s*null\\b|\\bnull\\s*==|\\bnull\\s*!=");
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1 && nullCheck.matcher(line).find()) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return methodLine;
                    }
                }
            }
        }
        return methodLine;
    }

    private List<Finding> mapClassLength(List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapLargeRecord(List<LargeRecordRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(), r.lineNumber(),
                        "Record '%s' has %d components — too many constructor parameters".formatted(
                                r.className(), r.componentCount())))
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
        // Recipe records lineNumber=-1; resolve via source-text by
        // matching the comment preview inside the method's body.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfCommentInMethod(r.className(), r.methodName(), r.commentPreview());
                    return finding(HeuristicCode.C3, r.className(), line,
                            "Mumbling comment in '%s': %s".formatted(r.methodName(), r.commentPreview()));
                })
                .toList();
    }

    /**
     * Finds the first comment inside the named method that contains the
     * given preview. Handles both standalone comment lines ({@code // foo})
     * and inline trailing comments ({@code code(); // foo}) — the
     * needle just needs to appear somewhere after a comment marker on
     * the line. Returns the method line if no match.
     */
    private int lineOfCommentInMethod(final String className, final String methodName,
                                       final String commentPreview) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        final String needle = commentPreview.substring(0, Math.min(30, commentPreview.length())).strip();
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1 && lineHasCommentContaining(line, needle)) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return methodLine;
                    }
                }
            }
        }
        return methodLine;
    }

    /**
     * Returns true when the line contains a comment ({@code //},
     * {@code /*}, or a continuation {@code *} after leading whitespace)
     * whose body includes {@code needle}. This accepts inline trailing
     * comments — {@code stmt(); // note} — which standalone-only checks
     * would reject.
     */
    private static boolean lineHasCommentContaining(final String line, final String needle) {
        if (needle.isEmpty()) {
            return false;
        }
        final int needleAt = line.indexOf(needle);
        if (needleAt < 0) {
            return false;
        }
        final int slashSlash = line.indexOf("//");
        if (slashSlash >= 0 && slashSlash < needleAt) {
            return true;
        }
        final int slashStar = line.indexOf("/*");
        if (slashStar >= 0 && slashStar < needleAt) {
            return true;
        }
        // Javadoc continuation: a line whose first non-space char is `*`
        // is part of a block comment. Anything to the right of that `*`
        // counts as comment body.
        final String stripped = line.stripLeading();
        if (stripped.startsWith("*")) {
            return true;
        }
        return false;
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
        // Row.declarationLine is the variable's offset within the method
        // body, NOT a file line. Resolve to the actual file line by
        // walking the method body for the first declaration of varName.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfLocalDeclaration(r.className(), r.methodName(), r.variableName());
                    return finding(HeuristicCode.G10, r.className(), line,
                            "'%s' is declared in %s() but not used until %d lines later — move the declaration closer to line %d"
                                    .formatted(r.variableName(), r.methodName(), r.distance(), r.firstUseLine()));
                })
                .toList();
    }

    /**
     * Source-text scan: returns the line number of the first local
     * variable declaration of {@code variableName} inside the named
     * method. Falls back to the method declaration line if no match.
     */
    private int lineOfLocalDeclaration(final String className, final String methodName,
                                       final String variableName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        // Heuristic for "this line declares a local named varName": some
        // word boundary, then varName, then = or ;. Skip lines that
        // begin with `*`, `//`, or `@` (Javadoc / annotations).
        final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(variableName) + "\\s*[=;]");
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String stripped = line.strip();
            if (enteredMethodBody && depth >= 1
                    && !stripped.startsWith("*") && !stripped.startsWith("//")
                    && !stripped.startsWith("@")
                    && decl.matcher(line).find()) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return methodLine;
                    }
                }
            }
        }
        return methodLine;
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
        // G25 in Clean Code is specifically about magic NUMBERS. Repeated
        // string literals are duplication — G5. The fix (extract to a
        // named constant) is the same shape, but the heuristic banner the
        // user reads should match the kind of smell.
        //
        // The recipe currently records lineNumber=-1 for each occurrence,
        // so we resolve the line ourselves by searching the source for
        // the first appearance of the literal.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFirstStringLiteral(r.className(), r.value());
                    return finding(HeuristicCode.G5, r.className(), line,
                            "String \"%s\" appears %d times — extract to a named constant".formatted(
                                    r.value(), r.count()));
                })
                .toList();
    }

    /**
     * Returns the source line of the first appearance of {@code "literal"}
     * in the file containing {@code className}, or -1 if the file isn't
     * readable. Skips comment lines so a Javadoc that mentions the literal
     * doesn't pull the snippet onto the wrong line.
     *
     * <p>Multi-line literals can't be matched directly: the recipe gives
     * us the unescaped value (e.g. {@code "});\n"}), but Java source
     * contains the escaped form (e.g. {@code "});\\n"}). Search using the
     * leading non-newline prefix of the literal — enough to identify the
     * line, even when the literal continues onto subsequent lines or is
     * built up via {@code + "..."} concatenation.
     */
    private int lineOfFirstStringLiteral(final String className, final String literal) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final String prefix = literalSearchPrefix(literal);
        if (prefix.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < lines.size(); i++) {
            final String stripped = lines.get(i).strip();
            if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                continue;
            }
            if (lines.get(i).contains(prefix)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Builds a search needle for the literal: a leading {@code "} plus
     * the literal's first chunk (up to a newline or 32 chars), re-escaped
     * the way Java source escapes string contents — backslash and quote
     * doubled, tab and newline as \t / \n. Without re-escaping, regex
     * literals like {@code "\s*\("} (real value) would never match the
     * source form {@code "\\s*\\("}.
     */
    static String literalSearchPrefix(final String literal) {
        if (literal == null || literal.isEmpty()) {
            return "";
        }
        final int firstNewline = literal.indexOf('\n');
        final int cutAt = firstNewline >= 0 ? firstNewline : Math.min(literal.length(), 32);
        final StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < cutAt; i++) {
            final char c = literal.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\t' -> out.append("\\t");
                case '\r' -> out.append("\\r");
                default -> out.append(c);
            }
        }
        return out.toString();
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
                .map(r -> finding(HeuristicCode.T1, r.className(), r.lineNumber(),
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

    private List<Finding> mapBadClassName(List<BadClassNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N1, r.className(),
                        "Class '%s' uses bad suffix '%s' — name after what it represents, not its role".formatted(
                                r.className(), r.suffix())))
                .toList();
    }

    private List<Finding> mapSystemOut(List<SystemOutRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G17, r.className(),
                        "'%s' belongs in a logger, not in business code — move to a structured log call".formatted(r.call())))
                .toList();
    }

    private List<Finding> mapFixedStringLog(List<FixedStringLogRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G12, r.className(),
                        "log.%s(\"%s\") has no runtime variables — delete the log line or upgrade it to a structured event".formatted(
                                r.level(), abbreviate(r.literal()))))
                .toList();
    }

    private static String abbreviate(final String s) {
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }

    private List<Finding> mapLegacyTypes(List<LegacyTypesRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G26, r.className(),
                        "'%s' is a legacy API — %s".formatted(r.legacyType(), r.replacement())))
                .toList();
    }

    private List<Finding> mapMultipleAssert(List<MultipleAssertRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.T1, r.className(), r.methodName(),
                        "Test '%s' has %d consecutive assertions — wrap in assertAll".formatted(
                                r.methodName(), r.assertCount())))
                .toList();
    }

    private List<Finding> mapLargeConstructor(List<LargeConstructorRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(),
                        "Constructor has %d parameters — introduce a parameter object or builder".formatted(
                                r.parameterCount())))
                .toList();
    }

    private List<Finding> mapInappropriateStatic(List<InappropriateStaticRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G18, r.className(), r.methodName(),
                        "Method '%s' does not use instance state — relocate to a more appropriate class or accept the coupling".formatted(
                                r.methodName())))
                .toList();
    }

    private List<Finding> mapStringlyTypedDispatch(List<StringlyTypedDispatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G23, r.className(), r.methodName(),
                        "Method '%s' dispatches on String parameter '%s' with %d branches — use an enum or split into separate methods".formatted(
                                r.methodName(), r.parameterName(), r.branchCount())))
                .toList();
    }

    private List<Finding> mapConfigurableData(List<ConfigurableDataRecipe.Row> rows) {
        return rows.stream()
                .map(r -> {
                    final int line = lineOfLiteralInMethod(r.className(), r.methodName(), r.literalValue());
                    return finding(HeuristicCode.G35, r.className(), line,
                            "Magic number %s in private method '%s' — extract to a named constant"
                                    .formatted(r.literalValue(), r.methodName()));
                })
                .toList();
    }

    /**
     * Finds the first line inside the named method's body that contains
     * the literal token. Used by G35 to anchor at the magic number's
     * actual line rather than the method declaration.
     */
    private int lineOfLiteralInMethod(final String className, final String methodName,
                                      final String literal) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1 && line.contains(literal)) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return methodLine;
                    }
                }
            }
        }
        return methodLine;
    }

    private List<Finding> mapEmbeddedLanguage(List<EmbeddedLanguageRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G1, r.className(), r.methodName(),
                        "Embedded %s in method '%s' — extract to a template or resource file".formatted(
                                r.language().toUpperCase(), r.methodName())))
                .toList();
    }

    private List<Finding> mapGuardClause(List<GuardClauseRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G30, r.className(), r.methodName(),
                        "Method '%s' has %d guard clauses — the entry conditions suggest it does several things".formatted(
                                r.methodName(), r.guardCount())))
                .toList();
    }

    private List<Finding> mapBaseClassDependency(List<BaseClassDependencyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G7, r.className(),
                        "'%s' depends on its derivative '%s' — invert the dependency".formatted(
                                r.className(), r.derivativeName())))
                .toList();
    }

    private List<Finding> mapArtificialCoupling(List<ArtificialCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G13, r.declaringClass(),
                        "Constant '%s' defined in '%s' but only used in '%s' — move it".formatted(
                                r.constantName(), r.declaringClass(), r.usedInClass())))
                .toList();
    }

    private List<Finding> mapHardcodedList(List<HardcodedListRecipe.Row> rows) {
        // The recipe doesn't record a line; locate the actual field
        // declaration ("<type> fieldName = " or "<type> fieldName;")
        // so the snippet shows the offending hardcoded list rather
        // than the class header.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFieldDeclaration(r.className(), r.fieldName());
                    return finding(HeuristicCode.G35, r.className(), line,
                            "'%s' is initialised with %d literal values outside a static-final field — extract to a constant table or enum".formatted(
                                    r.fieldName(), r.literalCount()));
                })
                .toList();
    }

    /**
     * First non-comment line in the file containing what looks like a
     * declaration of {@code fieldName} — i.e. the name followed by
     * {@code =} or {@code ;}. Returns -1 if not found, which lets
     * {@link #finding(HeuristicCode, String, int, String)} fall back
     * to the class line.
     */
    private int lineOfFieldDeclaration(final String className, final String fieldName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty() || fieldName == null || fieldName.isEmpty()) {
            return -1;
        }
        final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(fieldName) + "\\s*[=;]");
        for (int i = 0; i < lines.size(); i++) {
            final String stripped = lines.get(i).strip();
            if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                continue;
            }
            if (decl.matcher(lines.get(i)).find()) {
                return i + 1;
            }
        }
        return -1;
    }

    private List<Finding> mapSelectorArgument(List<SelectorArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G15, r.className(), r.methodName(),
                        "Method '%s' uses %s parameter '%s' to select behaviour — split into separate methods".formatted(
                                r.methodName(), r.parameterType(), r.parameterName())))
                .toList();
    }

    private List<Finding> mapObsoleteComment(List<ObsoleteCommentRecipe.Row> rows) {
        // Recipe doesn't record a line number — anchor at the first
        // comment line in the file that mentions the missing identifier.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfCommentMentioning(r.className(), r.missingIdentifier());
                    return finding(HeuristicCode.C2, r.className(), line,
                            "Comment references '%s' which is not in scope — update or remove".formatted(
                                    r.missingIdentifier()));
                })
                .toList();
    }

    /**
     * First line in the file containing a comment whose body includes
     * {@code identifier} as a whole word. Accepts inline trailing
     * comments and Javadoc continuation lines. Returns -1 if no match.
     */
    private int lineOfCommentMentioning(final String className, final String identifier) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty() || identifier == null || identifier.isEmpty()) {
            return -1;
        }
        final java.util.regex.Pattern wholeWord = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(identifier) + "\\b");
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (!wholeWord.matcher(line).find()) {
                continue;
            }
            if (lineHasCommentContaining(line, identifier)) {
                return i + 1;
            }
        }
        return -1;
    }

    private List<Finding> mapTemporalCoupling(List<TemporalCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G31, r.className(), r.methodName(),
                        "Method '%s' has %d consecutive void calls with no data dependency — make the order explicit".formatted(
                                r.methodName(), r.callCount())))
                .toList();
    }

    private List<Finding> mapBroadCatch(List<BroadCatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> {
                    final String sourcePath = resolveSourcePath(r.className());
                    final int line = lineOfCatchInMethod(r.className(), r.methodName(), r.caughtType());
                    final String message = "Method '%s' catches %s — catch specific exception types instead"
                            .formatted(r.methodName(), r.caughtType());
                    return Finding.at(HeuristicCode.Ch7_1, sourcePath, line, line,
                            message, severityFor(HeuristicCode.Ch7_1), Confidence.HIGH, TOOL,
                            HeuristicCode.Ch7_1.name());
                })
                .toList();
    }

    private List<Finding> mapRawGeneric(List<RawGenericRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G26, r.className(), r.methodName(),
                        "'%s' in '%s' uses Object type parameter — use a typed record or specific generic".formatted(
                                r.typeName(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSwallowedException(List<SwallowedExceptionRecipe.Row> rows) {
        return rows.stream()
                .map(r -> {
                    final String sourcePath = resolveSourcePath(r.className());
                    final int line = lineOfCatchInMethod(r.className(), r.methodName(), r.exceptionType());
                    final String message = "Method '%s' catches %s and silently swallows it — handle or propagate"
                            .formatted(r.methodName(), r.exceptionType());
                    return Finding.at(HeuristicCode.G4, sourcePath, line, line,
                            message, severityFor(HeuristicCode.G4), Confidence.HIGH, TOOL,
                            HeuristicCode.G4.name());
                })
                .toList();
    }

    private List<Finding> mapInconsistentReturn(List<InconsistentReturnRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F2, r.className(),
                        "Class has %d methods returning collections and %d void methods mutating collection params — pick one style".formatted(
                                r.returningMethods(), r.mutatingMethods())))
                .toList();
    }

    private List<Finding> mapSuppressedWarning(List<SuppressedWarningRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G4, r.className(), r.methodName(),
                        "@SuppressWarnings(\"%s\") on '%s' — redesign to avoid unsafe operations".formatted(
                                r.warningType(), r.methodName())))
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
        final int line = lineOfClass(className);
        return Finding.at(code, sourcePath, line, line,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private Finding finding(HeuristicCode code, String className, int line, String message) {
        final String sourcePath = resolveSourcePath(className);
        final int resolvedLine = line > 0 ? line : lineOfClass(className);
        return Finding.at(code, sourcePath, resolvedLine, resolvedLine,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private Finding findingForMethod(HeuristicCode code, String className, String methodName, String message) {
        final String sourcePath = resolveSourcePath(className);
        final int line = lineOfMethod(className, methodName);
        return Finding.at(code, sourcePath, line, line,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private String resolveSourcePath(String className) {
        return classNameToSourcePath.getOrDefault(className, className + ".java");
    }

    private int lineOfClass(final String className) {
        // Source-text scan first: AST line index lands on the closing
        // `*/` of the Javadoc above the class, not on the class line
        // itself. Walk source for the actual `class|record|interface|
        // enum <name>` token so the snippet anchors where the user
        // expects.
        final List<String> lines = readSourceLines(className);
        if (lines != null && !lines.isEmpty()) {
            final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                    "\\b(class|record|interface|enum)\\s+"
                            + java.util.regex.Pattern.quote(className)
                            + "\\b");
            for (int i = 0; i < lines.size(); i++) {
                final String stripped = lines.get(i).strip();
                if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                    continue;
                }
                if (decl.matcher(lines.get(i)).find()) {
                    return i + 1;
                }
            }
        }
        final J.CompilationUnit cu = classNameToCompilationUnit.get(className);
        if (cu == null) {
            return -1;
        }
        return cu.getClasses().stream()
                .filter(c -> c.getSimpleName().equals(className))
                .findFirst()
                .map(c -> lineIndexOf(cu).getOrDefault(c.getId(), -1))
                .orElse(-1);
    }

    /**
     * Returns the source line of the first {@code catch} clause whose
     * exception simple-name matches {@code exceptionType} inside a method
     * named {@code methodName} in the file containing {@code className}.
     *
     * <p>Source-text-driven on purpose. The AST line index accumulates
     * drift across methods, so anchoring at AST node lines is unreliable.
     * Instead, find the method declaration via a regex on actual source
     * text, then scan its body with brace-depth tracking for the first
     * matching catch keyword.
     *
     * <p>Falls back to a best-guess method line if no matching catch is
     * found or the source can't be read.
     */
    private int lineOfCatchInMethod(final String className, final String methodName,
                                    final String exceptionType) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return lineOfMethod(className, methodName);
        }
        final String wantedSimple = simpleName(exceptionType);
        // Method declaration regex: any modifiers, optional generic +
        // return type, then `methodName(`. We only need to anchor at the
        // method's opening line; brace tracking handles the rest.
        final java.util.regex.Pattern methodDecl = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(");
        final java.util.regex.Pattern catchPattern = java.util.regex.Pattern.compile(
                "\\bcatch\\s*\\([^)]*\\b"
                        + java.util.regex.Pattern.quote(wantedSimple)
                        + "\\b");

        for (int startIdx = 0; startIdx < lines.size(); startIdx++) {
            if (!methodDecl.matcher(lines.get(startIdx)).find()) {
                continue;
            }
            // Skip method calls / declarations that aren't real method
            // openings: they should contain `methodName(` AND end with `{`
            // or at least look like a declaration. The brace walker below
            // will exit immediately on a non-method match (depth never
            // increases), so we'd just continue scanning — keep this
            // permissive.
            final int catchLine = scanCatchInMethodBody(lines, startIdx, catchPattern);
            if (catchLine > 0) {
                return catchLine;
            }
        }
        return lineOfMethod(className, methodName);
    }

    private static int scanCatchInMethodBody(final List<String> lines, final int startIdx,
                                              final java.util.regex.Pattern catchPattern) {
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = startIdx; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1
                    && catchPattern.matcher(line).find()) {
                return i + 1;
            }
            for (int j = 0; j < line.length(); j++) {
                final char c = line.charAt(j);
                if (c == '{') {
                    depth++;
                    enteredMethodBody = true;
                } else if (c == '}') {
                    depth--;
                    if (enteredMethodBody && depth == 0) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private static String simpleName(final String fullyQualifiedOrSimple) {
        if (fullyQualifiedOrSimple == null || fullyQualifiedOrSimple.isEmpty()) {
            return fullyQualifiedOrSimple;
        }
        final int dot = fullyQualifiedOrSimple.lastIndexOf('.');
        return dot >= 0 ? fullyQualifiedOrSimple.substring(dot + 1) : fullyQualifiedOrSimple;
    }

    private final Map<String, List<String>> sourceLinesCache = new HashMap<>();

    private List<String> readSourceLines(final String className) {
        final String path = classNameToSourcePath.get(className);
        if (path == null) {
            return null;
        }
        return sourceLinesCache.computeIfAbsent(path, p -> {
            try {
                return Files.readAllLines(Path.of(p));
            } catch (IOException e) {
                return List.of();
            }
        });
    }

    private int lineOfMethod(final String className, final String methodName) {
        // Prefer source-text scan: AST line index drifts on lambdas,
        // multiline annotations, and other Spaces we don't fully count.
        // The text-based answer matches what the user sees in their editor.
        final int textLine = lineOfMethodFromSource(className, methodName);
        if (textLine > 0) {
            return textLine;
        }
        // Fallback to AST-based lookup if source can't be read.
        final J.CompilationUnit cu = classNameToCompilationUnit.get(className);
        if (cu == null) {
            return lineOfClass(className);
        }
        final Map<UUID, Integer> idx = lineIndexOf(cu);
        final int[] line = {-1};
        new JavaIsoVisitor<Object>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, Object o) {
                if (line[0] < 0 && methodName.equals(m.getSimpleName())) {
                    final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if (enclosing != null && className.equals(enclosing.getSimpleName())) {
                        line[0] = idx.getOrDefault(m.getId(), -1);
                    }
                }
                return super.visitMethodDeclaration(m, o);
            }
        }.visit(cu, new Object());
        return line[0] > 0 ? line[0] : lineOfClass(className);
    }

    /**
     * Source-text scan for the first line that looks like a method
     * declaration named {@code methodName}. Skips comment lines (so a
     * Javadoc {@code @link methodName} reference doesn't match) and
     * skips simple invocations (a line is treated as a declaration only
     * when {@code methodName(} is preceded by an identifier-or-modifier
     * token, e.g. {@code public void <name>(} or {@code Foo <name>(}).
     */
    private int lineOfMethodFromSource(final String className, final String methodName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        // Match `<token> methodName(` where <token> is the previous
        // chunk of the declaration (return type or modifier). Allow
        // `>` / `]` immediately before whitespace so generic return
        // types (`Map<String, String> name(`) and array returns
        // (`String[] name(`) match too. Lambdas (`x -> name(...)`) are
        // excluded by skipping any line that contains `->`. Lines
        // containing ` = ` before `methodName(` are assignments / call
        // sites — also skip.
        final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "[\\w>\\]]\\s+" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(");
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String stripped = line.strip();
            if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                continue;
            }
            if (line.contains("->") || line.contains(" = ")) {
                continue;
            }
            if (decl.matcher(line).find()) {
                return i + 1;
            }
        }
        return -1;
    }

    private final Map<UUID, Map<UUID, Integer>> lineIndexCache = new HashMap<>();

    private Map<UUID, Integer> lineIndexOf(final J.CompilationUnit cu) {
        return lineIndexCache.computeIfAbsent(cu.getId(), id -> buildLineIndex(cu));
    }

    private Map<UUID, Integer> buildLineIndex(final J.CompilationUnit cu) {
        final Map<UUID, Integer> index = new HashMap<>();
        final int[] line = {1};
        new JavaIsoVisitor<Object>() {
            @Override
            public J preVisit(final J node, final Object ignored) {
                line[0] += countNewlines(node.getPrefix().getWhitespace());
                for (final Comment c : node.getPrefix().getComments()) {
                    line[0] += countNewlines(c.printComment(getCursor()));
                }
                index.put(node.getId(), line[0]);
                return node;
            }

            @Override
            public J.Block visitBlock(final J.Block block, final Object o) {
                final J.Block result = super.visitBlock(block, o);
                // Block.end is the whitespace + comments between the last
                // statement and the closing `}`. preVisit only sees J nodes,
                // so without this hook the line counter never advances past
                // a method body's closing brace — every subsequent sibling
                // ends up on a wrong line. The drift compounds: each missed
                // brace shifts the next method one line earlier than truth.
                line[0] += countNewlines(result.getEnd().getWhitespace());
                for (final Comment c : result.getEnd().getComments()) {
                    line[0] += countNewlines(c.printComment(getCursor()));
                }
                return result;
            }
        }.visit(cu, new Object());
        return index;
    }

    private static int countNewlines(final String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }
}
