package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ShortVariableNameRecipe extends ScanningRecipe<ShortVariableNameRecipe.Accumulator> {

    private static final int MIN_NAME_LENGTH = 2;

    private static final Set<String> ALLOWED_SHORT_NAMES = Set.of(
            "i", "j", "k", "x", "y", "z", "e", "ex", "id");

    public record Row(String className, String methodName, String variableName,
                      String context, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Short variable name detection (N5)";
    }

    @Override
    public String getDescription() {
        return "Detects single-character variable names outside of loop iterators and lambdas.";
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
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations varDecls, ExecutionContext ctx) {
                final J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, ctx);

                v.getVariables().forEach(variable -> {
                    final String name = variable.getSimpleName();
                    if (name.length() >= MIN_NAME_LENGTH || ALLOWED_SHORT_NAMES.contains(name)) {
                        return;
                    }
                    if (isInsideForLoop() || isInsideLambda() || isMethodParameter()) {
                        return;
                    }
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            findEnclosingMethodName(),
                            name,
                            describeContext(v),
                            -1));
                });

                return v;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                m.getParameters().stream()
                        .filter(p -> p instanceof J.VariableDeclarations)
                        .map(p -> (J.VariableDeclarations) p)
                        .forEach(paramDecl -> paramDecl.getVariables().forEach(param -> {
                            final String name = param.getSimpleName();
                            if (name.length() >= MIN_NAME_LENGTH || ALLOWED_SHORT_NAMES.contains(name)) {
                                return;
                            }
                            acc.rows.add(new Row(
                                    findEnclosingClassName(),
                                    m.getSimpleName(),
                                    name,
                                    "parameter",
                                    -1));
                        }));

                return m;
            }

            private boolean isInsideForLoop() {
                return getCursor().firstEnclosing(J.ForLoop.class) != null
                        || getCursor().firstEnclosing(J.ForEachLoop.class) != null;
            }

            private boolean isInsideLambda() {
                return getCursor().firstEnclosing(J.Lambda.class) != null;
            }

            private boolean isMethodParameter() {
                return getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration;
            }

            private String describeContext(J.VariableDeclarations varDecls) {
                final String typeStr = varDecls.getType() != null
                        ? varDecls.getType().toString()
                        : "unknown";
                return "local variable of type " + typeStr;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }

            private String findEnclosingMethodName() {
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return methodDecl != null ? methodDecl.getSimpleName() : "<unknown>";
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
