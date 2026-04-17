package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class SuppressionParser {

    private static final String SUPPRESS_ANNOTATION = "SuppressCleanCode";
    private static final String SUPPRESS_LIST_ANNOTATION = "SuppressCleanCode.List";
    private static final String JAVA_EXTENSION = ".java";
    private static final String TOOL_NAME = "suppression-index";

    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<Finding> metaFindings = new ArrayList<>();
    private final JavaParser parser = new JavaParser();
    private final Path sourceRoot;

    SuppressionParser(final Path sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    ParsedSuppressions parse() {
        if (!Files.exists(sourceRoot)) {
            return new ParsedSuppressions(List.of(), List.of());
        }
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(p -> p.toString().endsWith(JAVA_EXTENSION)).forEach(this::parseFile);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to walk source root: " + sourceRoot, e);
        }
        return new ParsedSuppressions(List.copyOf(suppressions), List.copyOf(metaFindings));
    }

    private void parseFile(final Path file) {
        final ParseResult<CompilationUnit> result;
        try {
            result = parser.parse(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to parse: " + file, e);
        }
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            return;
        }
        final CompilationUnit cu = result.getResult().get();
        final String relativePath = deriveSourceFile(cu, file);
        processCompilationUnit(cu, relativePath);
    }

    private void processCompilationUnit(final CompilationUnit cu, final String sourceFile) {
        cu.getPackageDeclaration().ifPresent(pkg -> processPackageDeclaration(pkg, sourceFile));
        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(cls -> processAnnotatedNode(cls, sourceFile));
        cu.findAll(MethodDeclaration.class)
                .forEach(method -> processAnnotatedNode(method, sourceFile));
        cu.findAll(ConstructorDeclaration.class)
                .forEach(ctor -> processAnnotatedNode(ctor, sourceFile));
    }

    private void processPackageDeclaration(final PackageDeclaration pkg, final String sourceFile) {
        final String packagePath = pkg.getNameAsString().replace('.', '/');
        final int line = pkg.getBegin().map(p -> p.line).orElse(-1);
        final AnnotationSite site = new AnnotationSite(sourceFile, line, line, packagePath);
        for (final AnnotationExpr ann : pkg.getAnnotations()) {
            if (SUPPRESS_ANNOTATION.equals(ann.getNameAsString())) {
                processSingleAnnotation(ann, site);
            }
        }
    }

    private void processAnnotatedNode(final BodyDeclaration<?> node, final String sourceFile) {
        final int startLine = node.getBegin().map(p -> p.line).orElse(-1);
        final int endLine = node.getEnd().map(p -> p.line).orElse(-1);
        final AnnotationSite site = new AnnotationSite(sourceFile, startLine, endLine, null);

        for (final AnnotationExpr ann : node.getAnnotations()) {
            final String annName = ann.getNameAsString();
            if (SUPPRESS_ANNOTATION.equals(annName)) {
                processSingleAnnotation(ann, site);
            } else if (SUPPRESS_LIST_ANNOTATION.equals(annName)) {
                SuppressionAnnotationReader.expandRepeatableContainer(ann)
                        .forEach(inner -> processSingleAnnotation(inner, site));
            }
        }
    }

    private void processSingleAnnotation(final AnnotationExpr ann, final AnnotationSite site) {
        final SuppressionAnnotationReader.Values values = SuppressionAnnotationReader.read(ann);
        if (values.codes().isEmpty()) {
            return;
        }
        if (values.isExpired()) {
            metaFindings.add(metaFinding(ann, site.sourceFile(),
                    HeuristicCode.META_SUPPRESSION_EXPIRED, Severity.ERROR, "expired-suppression",
                    "Suppression expired on " + values.until() + " for " + values.codes()));
        }
        if (values.hasNoMeaningfulReason()) {
            metaFindings.add(metaFinding(ann, site.sourceFile(),
                    HeuristicCode.META_SUPPRESSION_NO_REASON, Severity.WARNING, "blank-reason",
                    "Suppression has no meaningful reason: '" + values.reason() + "'"));
        }
        suppressions.add(new Suppression(site.sourceFile(), site.startLine(), site.endLine(),
                values.codes(), values.reason(), values.until(), site.packagePath()));
    }

    private static Finding metaFinding(final AnnotationExpr ann, final String sourceFile,
                                       final HeuristicCode code, final Severity severity,
                                       final String ruleRef, final String message) {
        return Finding.at(code, sourceFile,
                ann.getBegin().map(p -> p.line).orElse(-1),
                ann.getEnd().map(p -> p.line).orElse(-1),
                message, severity, Confidence.HIGH, TOOL_NAME, ruleRef);
    }

    private static String deriveSourceFile(final CompilationUnit cu, final Path file) {
        final String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString().replace('.', '/'))
                .orElse("");
        final String fileName = file.getFileName().toString();
        return packageName.isEmpty() ? fileName : packageName + "/" + fileName;
    }

    record ParsedSuppressions(List<Suppression> suppressions, List<Finding> metaFindings) {
    }

    private record AnnotationSite(String sourceFile, int startLine, int endLine, String packagePath) {
    }
}
