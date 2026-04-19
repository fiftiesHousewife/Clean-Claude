package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LegacyFileApiRecipe extends ScanningRecipe<LegacyFileApiRecipe.Accumulator> {

    private static final Set<String> LEGACY_TYPES = Set.of(
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.FileReader",
            "java.io.FileWriter");

    public record Row(String className, String legacyType) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Legacy java.io.File API detection (G26 — Be Precise)";
    }

    @Override
    public String getDescription() {
        return "Detects usage of java.io.File and related legacy file APIs — use java.nio.file instead.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                    ExecutionContext ctx) {
                final J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                checkType(acc, v.getTypeExpression());
                return v;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                final J.NewClass nc = super.visitNewClass(newClass, ctx);
                final JavaType type = nc.getType();
                if (type instanceof JavaType.FullyQualified fq && LEGACY_TYPES.contains(fq.getFullyQualifiedName())) {
                    acc.rows.add(new Row(findEnclosingClassName(), fq.getClassName()));
                }
                return nc;
            }

            private void checkType(Accumulator acc, org.openrewrite.java.tree.TypeTree typeExpr) {
                if (typeExpr == null) {
                    return;
                }
                final JavaType type = typeExpr.getType();
                if (type instanceof JavaType.FullyQualified fq && LEGACY_TYPES.contains(fq.getFullyQualifiedName())) {
                    acc.rows.add(new Row(findEnclosingClassName(), fq.getClassName()));
                }
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
