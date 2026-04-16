package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

public class SuppressionIndex {

    private record Suppression(
            String sourceFile,
            int startLine,
            int endLine,
            Set<HeuristicCode> codes,
            String reason,
            String until,
            String packagePath
    ) {
        boolean isPackageScoped() {
            return packagePath != null;
        }
    }

    private final List<Suppression> suppressions;
    private final List<Finding> metaFindings;

    private SuppressionIndex(List<Suppression> suppressions, List<Finding> metaFindings) {
        this.suppressions = suppressions;
        this.metaFindings = metaFindings;
    }

    public static SuppressionIndex build(Path sourceRoot) {
        List<Suppression> suppressions = new ArrayList<>();
        List<Finding> metaFindings = new ArrayList<>();
        JavaParser parser = new JavaParser();

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                try {
                    ParseResult<CompilationUnit> result = parser.parse(file);
                    if (result.isSuccessful() && result.getResult().isPresent()) {
                        CompilationUnit cu = result.getResult().get();
                        String relativePath = deriveSourceFile(cu, file, sourceRoot);
                        processCompilationUnit(cu, relativePath, suppressions, metaFindings);
                    }
                } catch (IOException e) {
                    // Skip files that can't be parsed
                }
            });
        } catch (IOException e) {
            // Skip if source root can't be walked
        }

        return new SuppressionIndex(suppressions, metaFindings);
    }

    public boolean isSuppressed(Finding finding) {
        if (finding.sourceFile() == null) {
            return false;
        }

        for (Suppression s : suppressions) {
            if (!s.codes().contains(finding.code())) {
                continue;
            }
            if (isExpired(s)) {
                continue;
            }
            if (s.isPackageScoped()) {
                if (matchesPackage(finding.sourceFile(), s.packagePath())
                        || matchesPackage(otherFile(finding), s.packagePath())) {
                    return true;
                }
            } else if (matchesFile(finding.sourceFile(), s.sourceFile())
                    && finding.startLine() >= s.startLine()
                    && finding.startLine() <= s.endLine()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExpired(Suppression s) {
        if (s.until().isEmpty()) {
            return false;
        }
        return LocalDate.parse(s.until()).isBefore(LocalDate.now());
    }

    private static boolean matchesPackage(String path, String packagePath) {
        return path != null && path.contains(packagePath + "/");
    }

    private static String otherFile(Finding finding) {
        return finding.metadata() == null ? null : finding.metadata().get("otherFile");
    }

    public List<Finding> metaFindings() {
        return Collections.unmodifiableList(metaFindings);
    }

    private static void processCompilationUnit(CompilationUnit cu, String sourceFile,
                                                List<Suppression> suppressions,
                                                List<Finding> metaFindings) {
        cu.getPackageDeclaration().ifPresent(pkg ->
                processPackageDeclaration(pkg, sourceFile, suppressions, metaFindings));
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls ->
                processAnnotatedNode(cls, sourceFile, suppressions, metaFindings, null));
        cu.findAll(MethodDeclaration.class).forEach(method ->
                processAnnotatedNode(method, sourceFile, suppressions, metaFindings, null));
        cu.findAll(ConstructorDeclaration.class).forEach(ctor ->
                processAnnotatedNode(ctor, sourceFile, suppressions, metaFindings, null));
    }

    private static void processPackageDeclaration(com.github.javaparser.ast.PackageDeclaration pkg,
                                                   String sourceFile,
                                                   List<Suppression> suppressions,
                                                   List<Finding> metaFindings) {
        String packagePath = pkg.getNameAsString().replace('.', '/');
        int line = pkg.getBegin().map(p -> p.line).orElse(-1);
        for (AnnotationExpr ann : pkg.getAnnotations()) {
            if ("SuppressCleanCode".equals(ann.getNameAsString())) {
                processSingleAnnotation(ann, sourceFile, line, line, suppressions, metaFindings, packagePath);
            }
        }
    }

    private static void processAnnotatedNode(BodyDeclaration<?> node, String sourceFile,
                                              List<Suppression> suppressions,
                                              List<Finding> metaFindings,
                                              String packagePath) {
        int startLine = node.getBegin().map(p -> p.line).orElse(-1);
        int endLine = node.getEnd().map(p -> p.line).orElse(-1);

        for (AnnotationExpr ann : node.getAnnotations()) {
            String annName = ann.getNameAsString();

            if ("SuppressCleanCode".equals(annName)) {
                processSingleAnnotation(ann, sourceFile, startLine, endLine, suppressions, metaFindings, packagePath);
            } else if ("SuppressCleanCode.List".equals(annName)) {
                // Handle @Repeatable container
                if (ann instanceof NormalAnnotationExpr normal) {
                    for (MemberValuePair pair : normal.getPairs()) {
                        if ("value".equals(pair.getNameAsString())) {
                            pair.getValue().toArrayInitializerExpr().ifPresent(arr ->
                                    arr.getValues().forEach(v -> {
                                        if (v instanceof AnnotationExpr inner) {
                                            processSingleAnnotation(inner, sourceFile, startLine, endLine, suppressions, metaFindings, packagePath);
                                        }
                                    }));
                        }
                    }
                }
            }
        }
    }

    private static void processSingleAnnotation(AnnotationExpr ann, String sourceFile,
                                                  int startLine, int endLine,
                                                  List<Suppression> suppressions,
                                                  List<Finding> metaFindings,
                                                  String packagePath) {
        Set<HeuristicCode> codes = new HashSet<>();
        String reason = "";
        String until = "";

        if (ann instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                switch (pair.getNameAsString()) {
                    case "value" -> codes.addAll(extractCodes(pair.getValue()));
                    case "reason" -> reason = extractStringValue(pair.getValue());
                    case "until" -> until = extractStringValue(pair.getValue());
                }
            }
        }

        if (codes.isEmpty()) {
            return;
        }

        // Check for expired until
        if (!until.isEmpty()) {
            LocalDate untilDate = LocalDate.parse(until);
            if (untilDate.isBefore(LocalDate.now())) {
                metaFindings.add(Finding.at(
                        HeuristicCode.META_SUPPRESSION_EXPIRED, sourceFile,
                        ann.getBegin().map(p -> p.line).orElse(-1),
                        ann.getEnd().map(p -> p.line).orElse(-1),
                        "Suppression expired on " + until + " for " + codes,
                        Severity.ERROR, Confidence.HIGH,
                        "suppression-index", "expired-suppression"));
            }
        }

        // Check for blank/TODO reason
        if (reason.isBlank() || "TODO".equalsIgnoreCase(reason.trim())) {
            metaFindings.add(Finding.at(
                    HeuristicCode.META_SUPPRESSION_NO_REASON, sourceFile,
                    ann.getBegin().map(p -> p.line).orElse(-1),
                    ann.getEnd().map(p -> p.line).orElse(-1),
                    "Suppression has no meaningful reason: '" + reason + "'",
                    Severity.WARNING, Confidence.HIGH,
                    "suppression-index", "blank-reason"));
        }

        suppressions.add(new Suppression(sourceFile, startLine, endLine, codes, reason, until, packagePath));
    }

    private static String extractStringValue(Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return expr.asStringLiteralExpr().getValue();
        }
        if (expr.isBinaryExpr()) {
            final com.github.javaparser.ast.expr.BinaryExpr bin = expr.asBinaryExpr();
            if (bin.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS) {
                return extractStringValue(bin.getLeft()) + extractStringValue(bin.getRight());
            }
        }
        return expr.toString();
    }

    private static Set<HeuristicCode> extractCodes(Expression expr) {
        Set<HeuristicCode> codes = new HashSet<>();
        if (expr.isFieldAccessExpr()) {
            String name = expr.asFieldAccessExpr().getNameAsString();
            codes.add(HeuristicCode.valueOf(name));
        } else if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(v -> codes.addAll(extractCodes(v)));
        }
        return codes;
    }

    private static String deriveSourceFile(CompilationUnit cu, Path file, Path sourceRoot) {
        // Try to derive from package + filename
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString().replace('.', '/'))
                .orElse("");
        String fileName = file.getFileName().toString();
        return packageName.isEmpty() ? fileName : packageName + "/" + fileName;
    }

    private static boolean matchesFile(String findingFile, String suppressionFile) {
        // Support partial matching — finding file might have a different prefix
        return findingFile.equals(suppressionFile)
                || findingFile.endsWith(suppressionFile)
                || suppressionFile.endsWith(findingFile);
    }
}
