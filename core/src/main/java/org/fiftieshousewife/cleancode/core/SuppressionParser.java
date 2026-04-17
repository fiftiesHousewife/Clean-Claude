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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class SuppressionParser {

    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<Finding> metaFindings = new ArrayList<>();
    private final SuppressionAnnotationProcessor processor =
            new SuppressionAnnotationProcessor(suppressions, new SuppressionMetaFindings(metaFindings));
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
            final Optional<CompilationUnit> compilationUnit = result.getResult();
            if (result.isSuccessful() && compilationUnit.isPresent()) {
                processCompilationUnit(compilationUnit.get(), deriveSourceFile(compilationUnit.get(), file));
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
        processBodyDeclarations(cu, fileContext);
    }

    private void processBodyDeclarations(final CompilationUnit cu, final ParseContext context) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> processAnnotatedNode(cls, context));
        cu.findAll(MethodDeclaration.class).forEach(method -> processAnnotatedNode(method, context));
        cu.findAll(ConstructorDeclaration.class).forEach(ctor -> processAnnotatedNode(ctor, context));
    }

    private void processPackageDeclaration(final PackageDeclaration pkg, final String sourceFile) {
        final String packagePath = pkg.getNameAsString().replace('.', '/');
        final int line = pkg.getBegin().map(p -> p.line).orElse(-1);
        final ParseContext context = new ParseContext(sourceFile, packagePath);
        final LineRange range = new LineRange(line, line);
        for (final AnnotationExpr ann : pkg.getAnnotations()) {
            if (SuppressionAnnotationProcessor.SUPPRESS_ANNOTATION.equals(ann.getNameAsString())) {
                processor.processSingle(ann, context, range);
            }
        }
    }

    private void processAnnotatedNode(final BodyDeclaration<?> node, final ParseContext context) {
        final int startLine = node.getBegin().map(p -> p.line).orElse(-1);
        final int endLine = node.getEnd().map(p -> p.line).orElse(-1);
        final LineRange range = new LineRange(startLine, endLine);
        node.getAnnotations().forEach(ann -> processor.processAnnotation(ann, context, range));
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
}
