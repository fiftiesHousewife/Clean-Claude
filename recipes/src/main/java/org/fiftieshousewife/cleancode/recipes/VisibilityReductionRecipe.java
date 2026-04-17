package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisibilityReductionRecipe extends ScanningRecipe<VisibilityReductionRecipe.Accumulator> {

    public record Row(String className, String fieldName, String currentVisibility, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Visibility reduction detection (G8)";
    }

    @Override
    public String getDescription() {
        return "Detects public mutable (non-final, non-static) instance fields that should almost always be private.";
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
                final J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
                if (isReducibleVisibilityField(getCursor(), vd)) {
                    acc.rows.addAll(rowsFor(findEnclosingClassName(getCursor()), vd));
                }
                return vd;
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

    private static boolean isReducibleVisibilityField(org.openrewrite.Cursor cursor, J.VariableDeclarations vd) {
        return isFieldDeclaration(cursor) && isPublicInstanceMutableField(vd);
    }

    private static boolean isFieldDeclaration(org.openrewrite.Cursor cursor) {
        return cursor.firstEnclosing(J.MethodDeclaration.class) == null
                && cursor.firstEnclosing(J.ClassDeclaration.class) != null;
    }

    private static boolean isPublicInstanceMutableField(J.VariableDeclarations vd) {
        return hasModifier(vd, J.Modifier.Type.Public)
                && !hasModifier(vd, J.Modifier.Type.Static)
                && !hasModifier(vd, J.Modifier.Type.Final);
    }

    private static boolean hasModifier(J.VariableDeclarations vd, J.Modifier.Type type) {
        return vd.getModifiers().stream().anyMatch(mod -> mod.getType() == type);
    }

    private static String findEnclosingClassName(org.openrewrite.Cursor cursor) {
        final J.ClassDeclaration classDecl = cursor.firstEnclosing(J.ClassDeclaration.class);
        return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
    }

    private static List<Row> rowsFor(String className, J.VariableDeclarations vd) {
        return vd.getVariables().stream()
                .map(var -> new Row(className, var.getSimpleName(), "public", -1))
                .toList();
    }
}
