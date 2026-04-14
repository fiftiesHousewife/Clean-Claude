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

    private static final Set<HeuristicCode> COVERED = Set.of(
            HeuristicCode.F3, HeuristicCode.Ch7_1, HeuristicCode.G29,
            HeuristicCode.G36, HeuristicCode.G28, HeuristicCode.Ch7_2,
            HeuristicCode.Ch10_1, HeuristicCode.T3, HeuristicCode.T4);

    @Override
    public String id() {
        return "openrewrite";
    }

    @Override
    public String displayName() {
        return "OpenRewrite";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return COVERED;
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final List<Path> javaFiles = collectSourceFiles(context);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        final List<SourceFile> parsed = parseSourceFiles(javaFiles);
        final var flagArgs = new FlagArgumentRecipe();
        final var catchLog = new CatchLogContinueRecipe();
        final var negCond = new NegativeConditionalRecipe();
        final var demeter = new LawOfDemeterRecipe();
        final var encapCond = new EncapsulateConditionalRecipe();
        final var nullDensity = new NullDensityRecipe();
        final var classLength = new ClassLineLengthRecipe();
        final var disabledTest = new DisabledTestRecipe();

        List.of(flagArgs, catchLog, negCond, demeter, encapCond, nullDensity, classLength, disabledTest)
                .forEach(recipe -> runRecipe(parsed, recipe));

        final List<Finding> findings = new ArrayList<>();
        findings.addAll(mapFlagArgFindings(flagArgs.collectedRows()));
        findings.addAll(mapCatchLogFindings(catchLog.collectedRows()));
        findings.addAll(mapNegCondFindings(negCond.collectedRows()));
        findings.addAll(mapDemeterFindings(demeter.collectedRows()));
        findings.addAll(mapEncapCondFindings(encapCond.collectedRows()));
        findings.addAll(mapNullDensityFindings(nullDensity.collectedRows()));
        findings.addAll(mapClassLengthFindings(classLength.collectedRows()));
        findings.addAll(mapDisabledTestFindings(disabledTest.collectedRows()));
        return findings;
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

    private List<Finding> mapFlagArgFindings(List<FlagArgumentRecipe.FlagArgumentRow> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.F3, r.className(),
                        "Boolean parameter '%s' on method '%s'".formatted(r.paramName(), r.methodName())))
                .toList();
    }

    private List<Finding> mapCatchLogFindings(List<CatchLogContinueRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_1, r.className(),
                        "Catch block in '%s' only logs or is empty".formatted(r.methodName())))
                .toList();
    }

    private List<Finding> mapNegCondFindings(List<NegativeConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G29, r.className(),
                        "Double negation: %s".formatted(r.expression())))
                .toList();
    }

    private List<Finding> mapDemeterFindings(List<LawOfDemeterRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G36, r.className(),
                        "Method chain depth %d: %s".formatted(r.depth(), r.chain())))
                .toList();
    }

    private List<Finding> mapEncapCondFindings(List<EncapsulateConditionalRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.G28, r.className(),
                        "Complex condition (depth %d) should be extracted".formatted(r.depth())))
                .toList();
    }

    private List<Finding> mapNullDensityFindings(List<NullDensityRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch7_2, r.className(),
                        "Method '%s' has %d null checks".formatted(r.methodName(), r.nullCheckCount())))
                .toList();
    }

    private List<Finding> mapClassLengthFindings(List<ClassLineLengthRecipe.Row> rows) {
        return rows.stream()
                .map(r -> finding(HeuristicCode.Ch10_1, r.className(),
                        "Class '%s' is %d lines".formatted(r.className(), r.lineCount())))
                .toList();
    }

    private List<Finding> mapDisabledTestFindings(List<DisabledTestRecipe.Row> rows) {
        return rows.stream()
                .map(r -> Finding.at(HeuristicCode.T3, r.className() + ".java", -1, -1,
                        "@%s on '%s' without meaningful reason".formatted(r.annotation(), r.methodName()),
                        Severity.INFO, Confidence.HIGH, "openrewrite", "DisabledTestRecipe"))
                .toList();
    }

    private Finding finding(HeuristicCode code, String className, String message) {
        return Finding.at(code, className + ".java", -1, -1,
                message, Severity.WARNING, Confidence.HIGH, "openrewrite", code.name());
    }
}
