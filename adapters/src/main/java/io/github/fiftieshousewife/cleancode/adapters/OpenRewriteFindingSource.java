package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.FindingSource;
import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
import io.github.fiftieshousewife.cleancode.core.RecipeThresholds;
import io.github.fiftieshousewife.cleancode.core.Severity;
import io.github.fiftieshousewife.cleancode.recipes.ArtificialCouplingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.BadClassNameRecipe;
import io.github.fiftieshousewife.cleancode.recipes.BaseClassDependencyRecipe;
import io.github.fiftieshousewife.cleancode.recipes.BoundaryConditionRecipe;
import io.github.fiftieshousewife.cleancode.recipes.BroadCatchRecipe;
import io.github.fiftieshousewife.cleancode.recipes.CatchLogContinueRecipe;
import io.github.fiftieshousewife.cleancode.recipes.ClassLineLengthRecipe;
import io.github.fiftieshousewife.cleancode.recipes.CommentedCodeRecipe;
import io.github.fiftieshousewife.cleancode.recipes.ConfigurableDataRecipe;
import io.github.fiftieshousewife.cleancode.recipes.DisabledTestRecipe;
import io.github.fiftieshousewife.cleancode.recipes.EmbeddedLanguageRecipe;
import io.github.fiftieshousewife.cleancode.recipes.EncapsulateConditionalRecipe;
import io.github.fiftieshousewife.cleancode.recipes.EncodingNamingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.EnumForConstantsRecipe;
import io.github.fiftieshousewife.cleancode.recipes.FeatureEnvyRecipe;
import io.github.fiftieshousewife.cleancode.recipes.FixedStringLogRecipe;
import io.github.fiftieshousewife.cleancode.recipes.FlagArgumentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.FullyQualifiedReferenceRecipe;
import io.github.fiftieshousewife.cleancode.recipes.GuardClauseRecipe;
import io.github.fiftieshousewife.cleancode.recipes.HardcodedListRecipe;
import io.github.fiftieshousewife.cleancode.recipes.ImperativeLoopRecipe;
import io.github.fiftieshousewife.cleancode.recipes.InappropriateStaticRecipe;
import io.github.fiftieshousewife.cleancode.recipes.InconsistentNamingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.InconsistentReturnRecipe;
import io.github.fiftieshousewife.cleancode.recipes.InheritConstantsRecipe;
import io.github.fiftieshousewife.cleancode.recipes.LargeConstructorRecipe;
import io.github.fiftieshousewife.cleancode.recipes.LargeRecordRecipe;
import io.github.fiftieshousewife.cleancode.recipes.LawOfDemeterRecipe;
import io.github.fiftieshousewife.cleancode.recipes.LegacyTypesRecipe;
import io.github.fiftieshousewife.cleancode.recipes.MagicStringRecipe;
import io.github.fiftieshousewife.cleancode.recipes.MissingExplanatoryVariableRecipe;
import io.github.fiftieshousewife.cleancode.recipes.MultipleAssertRecipe;
import io.github.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.NegativeConditionalRecipe;
import io.github.fiftieshousewife.cleancode.recipes.NestedTernaryRecipe;
import io.github.fiftieshousewife.cleancode.recipes.NullDensityRecipe;
import io.github.fiftieshousewife.cleancode.recipes.ObsoleteCommentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.OutputArgumentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.PrivateMethodTestabilityRecipe;
import io.github.fiftieshousewife.cleancode.recipes.RawGenericRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SectionCommentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SelectorArgumentRecipe;
import io.github.fiftieshousewife.cleancode.recipes.ShortVariableNameRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SideEffectNamingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.StringBuilderThreadingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.StringSwitchRecipe;
import io.github.fiftieshousewife.cleancode.recipes.StringlyTypedDispatchRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SuppressedWarningRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SwallowedExceptionRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SwitchOnTypeRecipe;
import io.github.fiftieshousewife.cleancode.recipes.SystemOutRecipe;
import io.github.fiftieshousewife.cleancode.recipes.TemporalCouplingRecipe;
import io.github.fiftieshousewife.cleancode.recipes.UncheckedCastRecipe;
import io.github.fiftieshousewife.cleancode.recipes.VerticalSeparationRecipe;
import io.github.fiftieshousewife.cleancode.recipes.VisibilityReductionRecipe;
import io.github.fiftieshousewife.cleancode.recipes.WhitespaceSplitMethodRecipe;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
            HeuristicCode.G37,
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
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
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

    private Map<String, String> buildSourcePathIndex(final List<SourceFile> parsed) {
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
    private List<Finding> extractFindings(final List<ScanningRecipe<?>> recipes) {
        final List<Finding> findings = new ArrayList<>();
        recipes.forEach(recipe -> findings.addAll(mapRecipe(recipe)));
        return dedupCatchFindings(findings);
    }

    /**
     * A single catch block can attract multiple recipes: BroadCatchRecipe
     * (Ch7.1) for `catch (Exception e)`, and SwallowedExceptionRecipe (G4)
     * for an empty body. When both fire on the same line G4 wins — it
     * names the bigger smell. Drop the Ch7.1 finding to avoid the user
     * seeing the same catch under two heuristics.
     */
    private List<Finding> dedupCatchFindings(final List<Finding> findings) {
        record FileLine(String file, int line) {}
        final java.util.Set<FileLine> g4Lines = findings.stream()
                .filter(f -> f.code() == HeuristicCode.G4)
                .map(f -> new FileLine(f.sourceFile(), f.startLine()))
                .collect(java.util.stream.Collectors.toSet());
        return findings.stream()
                .filter(f -> !(f.code() == HeuristicCode.Ch7_1
                        && g4Lines.contains(new FileLine(f.sourceFile(), f.startLine()))))
                .toList();
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

    private List<Finding> mapStringBuilderThreading(final List<StringBuilderThreadingRecipe.Row> rows) {
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

    private List<Finding> mapFullyQualifiedReferences(final List<FullyQualifiedReferenceRecipe.Row> rows) {
        // Resolve each occurrence to the source line where it appears,
        // then group by (sourceFile, line) so multiple FQ refs on the
        // same line collapse to one finding (with the FQ list in the
        // message). Different lines stay as separate findings, so the
        // user sees each occurrence with its own snippet.
        record FileLine(String file, int line) {}
        final java.util.Map<FileLine, List<String>> grouped = new java.util.LinkedHashMap<>();
        rows.forEach(r -> {
            final int line = lineOfFqInSource(r.sourceFile(), r.fqText());
            if (line <= 0) {
                return;
            }
            grouped.computeIfAbsent(new FileLine(r.sourceFile(), line), k -> new ArrayList<>())
                    .add(r.fqText());
        });
        return grouped.entrySet().stream()
                .map(e -> {
                    final FileLine key = e.getKey();
                    final List<String> distinctRefs = e.getValue().stream().distinct().toList();
                    final String preview = distinctRefs.size() == 1
                            ? distinctRefs.getFirst()
                            : distinctRefs.size() + " refs (" + String.join(", ",
                                    distinctRefs.subList(0, Math.min(3, distinctRefs.size())))
                                    + (distinctRefs.size() > 3 ? ", ..." : "") + ")";
                    return Finding.at(HeuristicCode.G12, key.file(), key.line(), key.line(),
                            "Inline fully-qualified type reference: " + preview,
                            Severity.WARNING, Confidence.HIGH, TOOL, "FullyQualifiedReferenceRecipe");
                })
                .toList();
    }

    /**
     * First non-comment, non-import line in {@code sourceFile} that
     * contains the fully-qualified text. The recipe excludes import
     * lines from emission, but be defensive — if the only match is an
     * import, return -1 so the finding is dropped (it'd anchor on a
     * legitimate import).
     */
    private int lineOfFqInSource(final String sourceFile, final String fqText) {
        if (fqText == null || fqText.isEmpty()) {
            return -1;
        }
        final List<String> lines = readSourceLinesByPath(sourceFile);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String stripped = line.strip();
            if (stripped.startsWith("import ") || stripped.startsWith("package ")) {
                continue;
            }
            if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                continue;
            }
            if (line.contains(fqText)) {
                return i + 1;
            }
        }
        return -1;
    }

    private List<String> readSourceLinesByPath(final String relativeOrAbsolute) {
        final Path candidate = Path.of(relativeOrAbsolute);
        try {
            if (Files.exists(candidate)) {
                return Files.readAllLines(candidate);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private List<Finding> mapFlagArgs(final List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.F3, r.className(), r.methodName(), r.paramCount(),
                        "Method '%s' takes boolean parameter '%s' — split into two methods instead".formatted(r.methodName(), r.paramName())))
                .toList();
    }

    private List<Finding> mapOutputArgs(final List<OutputArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.F2, r.className(), r.methodName(),
                        "Method '%s' mutates its argument '%s' (%s) — return the result instead".formatted(
                                r.methodName(), r.paramName(), r.paramType())))
                .toList();
    }

    private List<Finding> mapCatchLog(final List<CatchLogContinueRecipe.Row> rows) {
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

    private List<Finding> mapNegCond(final List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(), r.lineNumber(),
                        "Double negation: %s".formatted(r.expression())))
                .toList();
    }

    private List<Finding> mapDemeter(final List<LawOfDemeterRecipe.Row> rows) {
        // Anchor at the actual chain expression inside the named method
        // so the snippet shows `a.b().c().d()`, not just the class
        // declaration. Falls back to the method line, then the class.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfChainInMethod(r.className(), r.methodName(), r.chain());
                    return finding(HeuristicCode.G36, r.className(), line,
                            "Method chain depth %d: %s".formatted(r.depth(), r.chain()));
                })
                .toList();
    }

    /**
     * Source-text scan: returns the first line inside {@code methodName}'s
     * body that contains the chain string (or its salient prefix). Falls
     * back to the method declaration line if no match.
     */
    private int lineOfChainInMethod(final String className, final String methodName,
                                     final String chain) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        // Match the first segment of the chain (everything up to the
        // first space) — chain strings can contain whitespace + arrows
        // we summarised but the source uses concrete syntax.
        final String needle = chain == null ? null : firstChainSegment(chain);
        if (needle == null || needle.isEmpty()) {
            return methodLine;
        }
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1 && line.contains(needle)) {
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
     * First syntactically-meaningful segment of a chain string (e.g.
     * "a.b().c()" → "a.b()"). Used as the search needle so we don't
     * try to match arrow-summarised chain strings against concrete
     * source syntax.
     */
    private static String firstChainSegment(final String chain) {
        final int firstSpace = chain.indexOf(' ');
        final String head = firstSpace > 0 ? chain.substring(0, firstSpace) : chain;
        // Trim arrow / bullet / ellipsis chars the recipe may insert.
        return head.replaceAll("[→\\u2026.…]+$", "");
    }

    private List<Finding> mapEncapCond(final List<EncapsulateConditionalRecipe.Row> rows) {
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

    private List<Finding> mapNullDensity(final List<NullDensityRecipe.Row> rows) {
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

    private List<Finding> mapClassLength(final List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapLargeRecord(final List<LargeRecordRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(), r.lineNumber(),
                        "Record '%s' has %d components — too many constructor parameters".formatted(
                                r.className(), r.componentCount())))
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

    private List<Finding> mapSectionComment(final List<SectionCommentRecipe.Row> rows) {
        // Recipe lineNumber=-1; anchor at the method holding the section
        // comments instead of letting all G34 collapse onto the class.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G34, r.className(), r.methodName(),
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

    private List<Finding> mapInheritConstants(final List<InheritConstantsRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.J2, r.className(), r.lineNumber(),
                        "Class inherits constants from interface '%s'".formatted(r.interfaceName())))
                .toList();
    }

    private List<Finding> mapEnumForConstants(final List<EnumForConstantsRecipe.Row> rows) {
        // Recipe records lineNumber=-1; resolve to the first
        // `<prefix>_<word>` field declaration line so the snippet
        // shows the offending constants, not the class header.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFieldWithPrefix(r.className(), r.prefix());
                    return finding(HeuristicCode.J3, r.className(), line,
                            "%d static final fields with prefix '%s' should be an enum".formatted(
                                    r.fieldCount(), r.prefix()));
                })
                .toList();
    }

    /**
     * First non-comment line in {@code className}'s file containing what
     * looks like a declaration of a {@code <prefix>_<word>} identifier.
     * Used by J3 to land on the offending constants, not the class
     * declaration. Returns -1 if no match.
     */
    private int lineOfFieldWithPrefix(final String className, final String prefix) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty() || prefix == null || prefix.isEmpty()) {
            return -1;
        }
        final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(prefix) + "_\\w+\\s*[=;]");
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

    private List<Finding> mapShortNames(final List<ShortVariableNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N5, r.className(), r.lineNumber(),
                        "'%s' in %s() is not a meaningful name — rename to reveal intent (%s)".formatted(
                                r.variableName(), r.methodName(), r.context())))
                .toList();
    }

    private List<Finding> mapMagicStrings(final List<MagicStringRecipe.Row> rows) {
        // Clean Code's G25 covers magic NUMBERS specifically; G5 covers
        // CPD-style block duplication. Repeated string literals share G25's
        // fix shape (extract to a named constant) but neither code is a
        // good fit on its own, so we route them to G37 — a project
        // extension that keeps block-duplication (G5) and string-literal
        // duplication separate, since they need different fixes.
        //
        // The recipe currently records lineNumber=-1 for each occurrence,
        // so we resolve the line ourselves by searching the source for
        // the first appearance of the literal.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFirstStringLiteral(r.className(), r.value());
                    return finding(HeuristicCode.G37, r.className(), line,
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

    private List<Finding> mapWhitespaceSplit(final List<WhitespaceSplitMethodRecipe.Row> rows) {
        // Recipe lineNumber=-1; anchor at the method itself.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G30, r.className(), r.methodName(),
                        "Method '%s' has %d blank-line sections across %d lines — each section should be its own method".formatted(
                                r.methodName(), r.blankLineCount(), r.totalLines())))
                .toList();
    }

    private List<Finding> mapPrivateMethod(final List<PrivateMethodTestabilityRecipe.PrivateMethodTestabilityRow> rows) {
        // The recipe records lineNumber=-1, so finding(code, className,
        // -1, ...) used to fall back to lineOfClass, putting the snippet
        // on the class declaration. Resolve via the source-text method
        // scan instead so T1 lands on the private method itself.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.T1, r.className(), r.methodName(),
                        "Private method '%s' (%d lines) should be package-private so it can be tested directly".formatted(
                                r.methodName(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapStringSwitch(final List<StringSwitchRecipe.Row> rows) {
        // Recipe records lineNumber=-1; without resolution every G23 on a
        // class lands on the class declaration. Scan inside the method's
        // body for `switch (selector)` so the snippet shows the switch.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfSwitchInMethod(r.className(), r.methodName(), r.selectorName());
                    return finding(HeuristicCode.G23, r.className(), line,
                            "Switch on String '%s' with %d cases — replace with an enum that encapsulates the behaviour".formatted(
                                    r.selectorName(), r.caseCount()));
                })
                .toList();
    }

    private int lineOfSwitchInMethod(final String className, final String methodName,
                                      final String selectorName) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final int methodLine = lineOfMethodFromSource(className, methodName);
        if (methodLine <= 0) {
            return -1;
        }
        final java.util.regex.Pattern switchPattern = java.util.regex.Pattern.compile(
                "\\bswitch\\s*\\(\\s*" + java.util.regex.Pattern.quote(selectorName) + "\\b");
        int depth = 0;
        boolean enteredMethodBody = false;
        for (int i = methodLine - 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (enteredMethodBody && depth >= 1 && switchPattern.matcher(line).find()) {
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

    private List<Finding> mapVisibility(final List<VisibilityReductionRecipe.Row> rows) {
        // Recipe records lineNumber=-1; without a real line every G8 on
        // the same class collapses onto the class declaration and looks
        // like duplicates. Resolve to the actual field declaration line.
        return rows.stream()
                .map(r -> {
                    final int line = lineOfFieldDeclaration(r.className(), r.fieldName());
                    return finding(HeuristicCode.G8, r.className(), line,
                            "Field '%s' is %s and mutable — should be private".formatted(
                                    r.fieldName(), r.currentVisibility()));
                })
                .toList();
    }

    private List<Finding> mapImperativeLoop(final List<ImperativeLoopRecipe.Row> rows) {
        // Recipe lineNumber=-1; route through findingForMethod so the
        // anchor is the actual method, not the class header.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G30, r.className(), r.methodName(),
                        "Loop in '%s' (%s) can be replaced with a stream operation".formatted(
                                r.methodName(), r.loopPattern())))
                .toList();
    }

    private List<Finding> mapUncheckedCast(final List<UncheckedCastRecipe.Row> rows) {
        // Recipe lineNumber=-1; memberName can be a method or a field.
        // Try the method line first (covers most @SuppressWarnings on
        // methods); fall back to field declaration; final fallback is
        // the class line.
        return rows.stream()
                .map(r -> {
                    int line = lineOfMethodFromSource(r.className(), r.memberName());
                    if (line <= 0) {
                        line = lineOfFieldDeclaration(r.className(), r.memberName());
                    }
                    return finding(HeuristicCode.G4, r.className(), line,
                            "@SuppressWarnings(\"unchecked\") on '%s' — redesign to avoid unsafe casts".formatted(
                                    r.memberName()));
                })
                .toList();
    }

    private List<Finding> mapFeatureEnvy(final List<FeatureEnvyRecipe.Row> rows) {
        // Recipe lineNumber=-1; anchor at the envying method.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G14, r.className(), r.methodName(),
                        "Method '%s' calls %d methods on '%s' but only %d on its own class — it wants to live elsewhere".formatted(
                                r.methodName(), r.externalCallCount(), r.enviedClass(), r.selfCallCount())))
                .toList();
    }

    private List<Finding> mapNestedTernary(final List<NestedTernaryRecipe.Row> rows) {
        // Recipe lineNumber=-1; without resolution every G16 collapses
        // onto the class declaration. Anchor at the method that holds the
        // ternary so the snippet shows the offending expression in
        // context.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G16, r.className(), r.methodName(),
                        "Ternary nested %d deep in '%s' — extract to an if/else or a named method".formatted(
                                r.depth(), r.methodName())))
                .toList();
    }

    private List<Finding> mapMissingExplanatory(final List<MissingExplanatoryVariableRecipe.Row> rows) {
        // Recipe lineNumber=-1; without resolution every G19 collapses
        // onto the class declaration. Anchor at the enclosing method.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G19, r.className(), r.methodName(),
                        "Complex expression in '%s' should be extracted to a named variable: %s".formatted(
                                r.methodName(), r.expressionPreview())))
                .toList();
    }

    private List<Finding> mapBoundaryCondition(final List<BoundaryConditionRecipe.Row> rows) {
        // Recipe lineNumber=-1; anchor at the method holding the boundary
        // expression.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G33, r.className(), r.methodName(),
                        "Boundary adjustment '%s' in '%s' — extract to a named variable".formatted(
                                r.expression(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSideEffectNaming(final List<SideEffectNamingRecipe.Row> rows) {
        // Recipe lineNumber=-1; anchor at the misnamed method.
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.N7, r.className(), r.methodName(),
                        "Method '%s' is named like a query but %s — rename to reveal the side effect".formatted(
                                r.methodName(), r.sideEffect())))
                .toList();
    }

    private List<Finding> mapInconsistentNaming(final List<InconsistentNamingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G11, r.className(), r.lineNumber(),
                        "Class uses inconsistent prefixes %s for the same concept: %s".formatted(
                                r.conflictingPrefixes(), r.methodNames())))
                .toList();
    }

    private List<Finding> mapBadClassName(final List<BadClassNameRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.N1, r.className(),
                        "Class '%s' uses bad suffix '%s' — name after what it represents, not its role".formatted(
                                r.className(), r.suffix())))
                .toList();
    }

    private List<Finding> mapSystemOut(final List<SystemOutRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G17, r.className(),
                        "'%s' belongs in a logger, not in business code — move to a structured log call".formatted(r.call())))
                .toList();
    }

    private List<Finding> mapFixedStringLog(final List<FixedStringLogRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G12, r.className(),
                        "log.%s(\"%s\") has no runtime variables — delete the log line or upgrade it to a structured event".formatted(
                                r.level(), abbreviate(r.literal()))))
                .toList();
    }

    private static String abbreviate(final String s) {
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }

    private List<Finding> mapLegacyTypes(final List<LegacyTypesRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G26, r.className(),
                        "'%s' is a legacy API — %s".formatted(r.legacyType(), r.replacement())))
                .toList();
    }

    private List<Finding> mapMultipleAssert(final List<MultipleAssertRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.T1, r.className(), r.methodName(),
                        "Test '%s' has %d consecutive assertions — wrap in assertAll".formatted(
                                r.methodName(), r.assertCount())))
                .toList();
    }

    private List<Finding> mapLargeConstructor(final List<LargeConstructorRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F1, r.className(),
                        "Constructor has %d parameters — introduce a parameter object or builder".formatted(
                                r.parameterCount())))
                .toList();
    }

    private List<Finding> mapInappropriateStatic(final List<InappropriateStaticRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G18, r.className(), r.methodName(),
                        "Method '%s' does not use instance state — relocate to a more appropriate class or accept the coupling".formatted(
                                r.methodName())))
                .toList();
    }

    private List<Finding> mapStringlyTypedDispatch(final List<StringlyTypedDispatchRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G23, r.className(), r.methodName(),
                        "Method '%s' dispatches on String parameter '%s' with %d branches — use an enum or split into separate methods".formatted(
                                r.methodName(), r.parameterName(), r.branchCount())))
                .toList();
    }

    private List<Finding> mapConfigurableData(final List<ConfigurableDataRecipe.Row> rows) {
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

    private List<Finding> mapEmbeddedLanguage(final List<EmbeddedLanguageRecipe.Row> rows) {
        // Roll up by (className, language) so a file with thirty
        // append("<html>") methods produces ONE G1, not thirty.
        // Anchor at the first literal's line so the snippet shows the
        // actual embedded fragment instead of the method declaration.
        final java.util.Map<String, List<EmbeddedLanguageRecipe.Row>> grouped =
                new java.util.LinkedHashMap<>();
        rows.forEach(r -> grouped
                .computeIfAbsent(r.className() + "/" + r.language(), k -> new ArrayList<>())
                .add(r));
        return grouped.values().stream()
                .map(group -> {
                    final EmbeddedLanguageRecipe.Row first = group.getFirst();
                    final int line = lineOfFirstStringLiteral(first.className(), first.literalPreview());
                    final String methodList = group.stream()
                            .map(EmbeddedLanguageRecipe.Row::methodName)
                            .distinct()
                            .limit(5)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(first.methodName());
                    final int extra = group.size() - 5;
                    final String suffix = extra > 0 ? " (+" + extra + " more)" : "";
                    return finding(HeuristicCode.G1, first.className(), line,
                            "Embedded %s across %d method(s) in '%s': %s%s — extract to a template or resource file"
                                    .formatted(first.language().toUpperCase(), group.size(),
                                            first.className(), methodList, suffix));
                })
                .toList();
    }

    private List<Finding> mapGuardClause(final List<GuardClauseRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G30, r.className(), r.methodName(),
                        "Method '%s' has %d guard clauses — the entry conditions suggest it does several things".formatted(
                                r.methodName(), r.guardCount())))
                .toList();
    }

    private List<Finding> mapBaseClassDependency(final List<BaseClassDependencyRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G7, r.className(),
                        "'%s' depends on its derivative '%s' — invert the dependency".formatted(
                                r.className(), r.derivativeName())))
                .toList();
    }

    private List<Finding> mapArtificialCoupling(final List<ArtificialCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G13, r.declaringClass(),
                        "Constant '%s' defined in '%s' but only used in '%s' — move it".formatted(
                                r.constantName(), r.declaringClass(), r.usedInClass())))
                .toList();
    }

    private List<Finding> mapHardcodedList(final List<HardcodedListRecipe.Row> rows) {
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

    private List<Finding> mapSelectorArgument(final List<SelectorArgumentRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G15, r.className(), r.methodName(),
                        "Method '%s' uses %s parameter '%s' to select behaviour — split into separate methods".formatted(
                                r.methodName(), r.parameterType(), r.parameterName())))
                .toList();
    }

    private List<Finding> mapObsoleteComment(final List<ObsoleteCommentRecipe.Row> rows) {
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

    private List<Finding> mapTemporalCoupling(final List<TemporalCouplingRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G31, r.className(), r.methodName(),
                        "Method '%s' has %d distinct void calls in sequence — make the order explicit".formatted(
                                r.methodName(), r.callCount())))
                .toList();
    }

    private List<Finding> mapBroadCatch(final List<BroadCatchRecipe.Row> rows) {
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

    private List<Finding> mapRawGeneric(final List<RawGenericRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G26, r.className(), r.methodName(),
                        "'%s' in '%s' uses Object type parameter — use a typed record or specific generic".formatted(
                                r.typeName(), r.methodName())))
                .toList();
    }

    private List<Finding> mapSwallowedException(final List<SwallowedExceptionRecipe.Row> rows) {
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

    private List<Finding> mapInconsistentReturn(final List<InconsistentReturnRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F2, r.className(),
                        "Class has %d methods returning collections and %d void methods mutating collection params — pick one style".formatted(
                                r.returningMethods(), r.mutatingMethods())))
                .toList();
    }

    private List<Finding> mapSuppressedWarning(final List<SuppressedWarningRecipe.Row> rows) {
        return rows.stream()
                .map(r -> findingForMethod(HeuristicCode.G4, r.className(), r.methodName(),
                        "@SuppressWarnings(\"%s\") on '%s' — redesign to avoid unsafe operations".formatted(
                                r.warningType(), r.methodName())))
                .toList();
    }

    private List<Path> collectSourceFiles(final ProjectContext context) {
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

    private List<SourceFile> parseSourceFiles(final List<Path> files) {
        return JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(files, null, new InMemoryExecutionContext(Throwable::printStackTrace))
                .toList();
    }

    private void runRecipe(final List<SourceFile> parsed, final ScanningRecipe<?> recipe) {
        final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
    }

    private Finding finding(final HeuristicCode code, final String className, final String message) {
        final String sourcePath = resolveSourcePath(className);
        final int line = lineOfClass(className);
        return Finding.at(code, sourcePath, line, line,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private Finding finding(final HeuristicCode code, final String className, final int line, final String message) {
        final String sourcePath = resolveSourcePath(className);
        final int resolvedLine = line > 0 ? line : lineOfClass(className);
        return Finding.at(code, sourcePath, resolvedLine, resolvedLine,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private Finding findingForMethod(final HeuristicCode code, final String className, final String methodName, final String message) {
        return findingForMethod(code, className, methodName, -1, message);
    }

    /**
     * Method-level finding, disambiguated by parameter count when the
     * caller knows it. Falls back to the first-match behaviour when
     * {@code paramCount} is negative.
     */
    private Finding findingForMethod(final HeuristicCode code, final String className, final String methodName,
                                      final int paramCount, final String message) {
        final String sourcePath = resolveSourcePath(className);
        final int line = lineOfMethod(className, methodName, paramCount);
        return Finding.at(code, sourcePath, line, line,
                message, severityFor(code), Confidence.HIGH, TOOL, code.name());
    }

    private String resolveSourcePath(final String className) {
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
        return lineOfMethod(className, methodName, -1);
    }

    private int lineOfMethod(final String className, final String methodName, final int paramCount) {
        // Prefer source-text scan: AST line index drifts on lambdas,
        // multiline annotations, and other Spaces we don't fully count.
        // The text-based answer matches what the user sees in their editor.
        final int textLine = lineOfMethodFromSource(className, methodName, paramCount);
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
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration m, final Object o) {
                if (line[0] < 0 && methodName.equals(m.getSimpleName())) {
                    if (paramCount >= 0 && m.getParameters().size() != paramCount) {
                        return super.visitMethodDeclaration(m, o);
                    }
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
        return lineOfMethodFromSource(className, methodName, -1);
    }

    /**
     * Same as {@link #lineOfMethodFromSource(String, String)}, but when
     * {@code paramCount >= 0} disambiguates between overloads by counting
     * top-level parameters in the declaration's parenthesised list.
     * Generics ({@code Map<String, Integer>}) are handled — only commas
     * outside of angle brackets and nested parens count as separators.
     * If no overload matches the requested arity the first by-name match
     * is returned, so partial information still does better than nothing.
     */
    private int lineOfMethodFromSource(final String className, final String methodName,
                                        final int paramCount) {
        final List<String> lines = readSourceLines(className);
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        final java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "[\\w>\\]]\\s+" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(");
        int firstMatchLine = -1;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String stripped = line.strip();
            if (stripped.startsWith("*") || stripped.startsWith("/*") || stripped.startsWith("//")) {
                continue;
            }
            if (line.contains("->") || line.contains(" = ")) {
                continue;
            }
            final java.util.regex.Matcher m = decl.matcher(line);
            if (!m.find()) {
                continue;
            }
            if (firstMatchLine < 0) {
                firstMatchLine = i + 1;
            }
            if (paramCount < 0) {
                return i + 1;
            }
            // Count parameters by walking from just after the `(` that
            // closes the regex match. The parameter list can wrap onto
            // subsequent lines; readUntilMatchingParen handles that.
            final int parenAt = m.end() - 1;
            if (countTopLevelParams(lines, i, parenAt) == paramCount) {
                return i + 1;
            }
        }
        return firstMatchLine;
    }

    /**
     * Counts top-level parameters inside the parenthesis whose opening
     * char is at {@code openLine.charAt(openCol)}. Walks lines forward
     * tracking paren / angle-bracket / brace depth. The parameter list
     * is empty when the first non-whitespace character is {@code )}.
     * Otherwise the count is one plus the number of commas at depth 0.
     */
    private static int countTopLevelParams(final List<String> lines,
                                            final int openLineIdx,
                                            final int openCol) {
        int parenDepth = 0;
        int angleDepth = 0;
        int braceDepth = 0;
        int commas = 0;
        boolean sawAnyContent = false;
        for (int i = openLineIdx; i < lines.size(); i++) {
            final String line = lines.get(i);
            final int from = (i == openLineIdx) ? openCol : 0;
            for (int j = from; j < line.length(); j++) {
                final char c = line.charAt(j);
                switch (c) {
                    case '(' -> parenDepth++;
                    case ')' -> {
                        parenDepth--;
                        if (parenDepth == 0) {
                            return sawAnyContent ? commas + 1 : 0;
                        }
                    }
                    case '<' -> angleDepth++;
                    case '>' -> {
                        if (angleDepth > 0) {
                            angleDepth--;
                        }
                    }
                    case '{' -> braceDepth++;
                    case '}' -> {
                        if (braceDepth > 0) {
                            braceDepth--;
                        }
                    }
                    case ',' -> {
                        if (parenDepth == 1 && angleDepth == 0 && braceDepth == 0) {
                            commas++;
                        }
                    }
                    default -> {
                        if (parenDepth >= 1 && !Character.isWhitespace(c)) {
                            sawAnyContent = true;
                        }
                    }
                }
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
