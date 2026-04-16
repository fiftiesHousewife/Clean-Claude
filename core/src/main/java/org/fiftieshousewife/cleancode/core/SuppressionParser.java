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
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class SuppressionParser {

    private static final String SUPPRESS_ANNOTATION = "SuppressCleanCode";
    private static final String SUPPRESS_LIST_ANNOTATION = "SuppressCleanCode.List";

    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<Finding> metaFindings = new ArrayList<>();
    private final SuppressionMetaFindings meta = new SuppressionMetaFindings(metaFindings);
    private final JavaParser parser = new JavaParser();

    static ParseOutcome parse(final Path sourceRoot) {
        final SuppressionParser instance = new SuppressionParser();
        instance.walk(sourceRoot);
        return new ParseOutcome(instance.suppressions, instance.metaFindings);
    }

    private void walk(final Path sourceRoot) {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(this::parseFile);
        } catch (IOException unreadableRoot) {
            skipUnreadable(sourceRoot, unreadableRoot);
        }
    }

    private void parseFile(final Path file) {
        try {
            final ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                final CompilationUnit cu = result.getResult().get();
                final String relativePath = deriveSourceFile(cu, file);
                processCompilationUnit(cu, relativePath);
            }
        } catch (IOException unreadableFile) {
            skipUnreadable(file, unreadableFile);
        }
    }

    /**
     * Suppression scanning is best-effort: an unreadable file or root
     * directory contributes no suppressions but must not abort the index
     * build. Lives in a named method so each catch block documents its
     * intent rather than being a silently empty block. Callers in
     * FindingFilterTest pass non-existent paths and rely on this.
     */
    @SuppressWarnings("unused")
    private void skipUnreadable(final Path path, final IOException cause) {
    }

    private void processCompilationUnit(final CompilationUnit cu, final String sourceFile) {
        cu.getPackageDeclaration().ifPresent(pkg -> processPackageDeclaration(pkg, sourceFile));
        final ParseContext fileContext = new ParseContext(sourceFile, null);
        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(cls -> processAnnotatedNode(cls, fileContext));
        cu.findAll(MethodDeclaration.class)
                .forEach(method -> processAnnotatedNode(method, fileContext));
        cu.findAll(ConstructorDeclaration.class)
                .forEach(ctor -> processAnnotatedNode(ctor, fileContext));
    }

    private void processPackageDeclaration(final PackageDeclaration pkg, final String sourceFile) {
        final String packagePath = pkg.getNameAsString().replace('.', '/');
        final int line = pkg.getBegin().map(p -> p.line).orElse(-1);
        final ParseContext context = new ParseContext(sourceFile, packagePath);
        final LineRange range = new LineRange(line, line);
        for (final AnnotationExpr ann : pkg.getAnnotations()) {
            if (SUPPRESS_ANNOTATION.equals(ann.getNameAsString())) {
                processSingleAnnotation(ann, context, range);
            }
        }
    }

    private void processAnnotatedNode(final BodyDeclaration<?> node, final ParseContext context) {
        final int startLine = node.getBegin().map(p -> p.line).orElse(-1);
        final int endLine = node.getEnd().map(p -> p.line).orElse(-1);
        final LineRange range = new LineRange(startLine, endLine);

        for (final AnnotationExpr ann : node.getAnnotations()) {
            final String annName = ann.getNameAsString();
            if (SUPPRESS_ANNOTATION.equals(annName)) {
                processSingleAnnotation(ann, context, range);
            } else if (SUPPRESS_LIST_ANNOTATION.equals(annName)) {
                processRepeatableContainer(ann, context, range);
            }
        }
    }

    private void processRepeatableContainer(final AnnotationExpr ann,
                                            final ParseContext context,
                                            final LineRange range) {
        if (!(ann instanceof NormalAnnotationExpr normal)) {
            return;
        }
        for (final MemberValuePair pair : normal.getPairs()) {
            if (!"value".equals(pair.getNameAsString())) {
                continue;
            }
            pair.getValue().toArrayInitializerExpr().ifPresent(arr ->
                    arr.getValues().forEach(v -> {
                        if (v instanceof AnnotationExpr inner) {
                            processSingleAnnotation(inner, context, range);
                        }
                    }));
        }
    }

    private void processSingleAnnotation(final AnnotationExpr ann,
                                         final ParseContext context,
                                         final LineRange range) {
        final Set<HeuristicCode> codes = new HashSet<>();
        String reason = "";
        String until = "";

        if (ann instanceof NormalAnnotationExpr normal) {
            for (final MemberValuePair pair : normal.getPairs()) {
                switch (pair.getNameAsString()) {
                    case "value" -> codes.addAll(AnnotationValues.extractCodes(pair.getValue()));
                    case "reason" -> reason = AnnotationValues.extractString(pair.getValue());
                    case "until" -> until = AnnotationValues.extractString(pair.getValue());
                }
            }
        }

        if (codes.isEmpty()) {
            return;
        }

        meta.recordIfExpired(ann, context.sourceFile(), codes, until);
        meta.recordIfBlankReason(ann, context.sourceFile(), reason);

        suppressions.add(new Suppression(
                context.sourceFile(), range.start(), range.end(),
                codes, reason, until, context.packagePath()));
    }

    private static String deriveSourceFile(final CompilationUnit cu, final Path file) {
        final String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString().replace('.', '/'))
                .orElse("");
        final String fileName = file.getFileName().toString();
        return packageName.isEmpty() ? fileName : packageName + "/" + fileName;
    }

    record ParseOutcome(List<Suppression> suppressions, List<Finding> metaFindings) {
    }

    private record ParseContext(String sourceFile, String packagePath) {
    }

    private record LineRange(int start, int end) {
    }
}
