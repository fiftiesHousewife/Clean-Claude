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

                if (!isFieldDeclaration()) {
                    return vd;
                }

                if (!isPublic(vd) || isStatic(vd) || isFinal(vd)) {
                    return vd;
                }

                final String className = findEnclosingClassName();
                for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                    acc.rows.add(new Row(className, var.getSimpleName(), "public", -1));
                }

                return vd;
            }

            private boolean isFieldDeclaration() {
                return getCursor().firstEnclosing(J.MethodDeclaration.class) == null
                        && getCursor().firstEnclosing(J.ClassDeclaration.class) != null;
            }

            private boolean isPublic(J.VariableDeclarations varDecls) {
                return varDecls.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Public);
            }

            private boolean isStatic(J.VariableDeclarations varDecls) {
                return varDecls.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
            }

            private boolean isFinal(J.VariableDeclarations varDecls) {
                return varDecls.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Final);
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
