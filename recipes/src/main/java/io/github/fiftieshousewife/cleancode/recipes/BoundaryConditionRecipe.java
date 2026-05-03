package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoundaryConditionRecipe extends ScanningRecipe<BoundaryConditionRecipe.Accumulator> {

    private static final Set<J.Binary.Type> BOUNDARY_OPERATORS = Set.of(
            J.Binary.Type.Addition, J.Binary.Type.Subtraction
    );

    public record Row(String className, String methodName, String expression, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Boundary condition detection (G33)";
    }

    @Override
    public String getDescription() {
        return "Detects raw +1/-1 boundary adjustments on method invocations that should be extracted to named variables.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            // Per Clean Code G33 the smell is a boundary expression
            // (`level + 1`) repeated in the same method body — pulling
            // it into a named variable removes the duplication. A
            // single `size() - 1` or `lastIndexOf('/') + 1` is the
            // idiomatic substring/last-element shape, not a smell.
            // Track occurrences per method and emit only when ≥2.
            private final Map<String, Integer> currentMethodCounts = new HashMap<>();
            private String currentMethodName;

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final String previousName = currentMethodName;
                currentMethodName = method.getSimpleName();
                final Map<String, Integer> previousCounts = new HashMap<>(currentMethodCounts);
                currentMethodCounts.clear();

                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                currentMethodCounts.forEach((expr, count) -> {
                    if (count >= 2) {
                        acc.rows.add(new Row(
                                findEnclosingClassName(),
                                m.getSimpleName(),
                                expr,
                                -1));
                    }
                });

                currentMethodCounts.clear();
                currentMethodCounts.putAll(previousCounts);
                currentMethodName = previousName;
                return m;
            }

            @Override
            public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
                final J.Binary b = super.visitBinary(binary, ctx);
                if (currentMethodName != null && isBoundaryExpression(b)) {
                    final String key = b.printTrimmed(getCursor());
                    currentMethodCounts.merge(key, 1, Integer::sum);
                }
                return b;
            }

            private boolean isBoundaryExpression(final J.Binary b) {
                if (!BOUNDARY_OPERATORS.contains(b.getOperator())) {
                    return false;
                }
                final boolean leftIsMethodCall = b.getLeft() instanceof J.MethodInvocation;
                final boolean rightIsMethodCall = b.getRight() instanceof J.MethodInvocation;
                final boolean leftIsLiteralOne = isLiteralOne(b.getLeft());
                final boolean rightIsLiteralOne = isLiteralOne(b.getRight());
                return (leftIsMethodCall && rightIsLiteralOne)
                        || (rightIsMethodCall && leftIsLiteralOne);
            }

            private boolean isLiteralOne(org.openrewrite.java.tree.Expression expr) {
                if (expr instanceof J.Literal literal) {
                    final Object value = literal.getValue();
                    return value instanceof Integer i && (i == 1 || i == -1);
                }
                if (expr instanceof J.Unary unary
                        && unary.getOperator() == J.Unary.Type.Negative
                        && unary.getExpression() instanceof J.Literal literal) {
                    final Object value = literal.getValue();
                    return value instanceof Integer i && i == 1;
                }
                return false;
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
