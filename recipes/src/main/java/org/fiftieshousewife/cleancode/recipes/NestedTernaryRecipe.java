package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NestedTernaryRecipe extends ScanningRecipe<NestedTernaryRecipe.Accumulator> {

    public record Row(
            String className,
            String methodName,
            int depth,
            int lineNumber
    ) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Nested ternary detection (G16)";
    }

    @Override
    public String getDescription() {
        return "Detects ternary expressions nested inside other ternary expressions.";
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
            public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
                final J.Ternary t = super.visitTernary(ternary, ctx);

                if (isNestedInsideTernary()) {
                    return t;
                }

                final int depth = maxNestingDepth(t);
                if (depth > 1) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            findEnclosingMethodName(),
                            depth,
                            -1));
                }

                return t;
            }

            private boolean isNestedInsideTernary() {
                Cursor cursor = getCursor().getParentTreeCursor();
                while (cursor != null && cursor.getValue() instanceof J) {
                    final Object value = cursor.getValue();
                    if (value instanceof J.Ternary) {
                        return true;
                    }
                    if (!(value instanceof J.Parentheses<?>)) {
                        return false;
                    }
                    cursor = cursor.getParentTreeCursor();
                }
                return false;
            }

            private int maxNestingDepth(J.Ternary ternary) {
                final int trueBranchDepth = depthOf(ternary.getTruePart());
                final int falseBranchDepth = depthOf(ternary.getFalsePart());
                return 1 + Math.max(trueBranchDepth, falseBranchDepth);
            }

            private int depthOf(Expression expression) {
                Expression unwrapped = expression;
                while (unwrapped instanceof J.Parentheses<?> parens) {
                    unwrapped = (Expression) parens.getTree();
                }
                if (unwrapped instanceof J.Ternary nested) {
                    return maxNestingDepth(nested);
                }
                return 0;
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
