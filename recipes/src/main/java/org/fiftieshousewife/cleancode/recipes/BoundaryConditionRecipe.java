package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BoundaryConditionRecipe extends ScanningRecipe<BoundaryConditionRecipe.Accumulator> {

    private static final Set<J.Binary.Type> BOUNDARY_OPERATORS = Set.of(
            J.Binary.Type.Addition, J.Binary.Type.Subtraction
    );

    private static final String UNKNOWN_NAME = "<unknown>";

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
        return "Detects raw +1/-1 boundary adjustments on method invocations that should be "
                + "extracted to named variables.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new BoundaryScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class BoundaryScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        BoundaryScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.Binary visitBinary(final J.Binary binary, final ExecutionContext ctx) {
            final J.Binary visited = super.visitBinary(binary, ctx);
            if (!BOUNDARY_OPERATORS.contains(visited.getOperator())) {
                return visited;
            }
            if (isMethodCallBoundaryAdjustment(visited)) {
                acc.rows.add(buildRow(visited));
            }
            return visited;
        }

        private Row buildRow(final J.Binary visited) {
            return new Row(
                    findEnclosingClassName(),
                    findEnclosingMethodName(),
                    visited.printTrimmed(getCursor()),
                    -1
            );
        }

        private boolean isMethodCallBoundaryAdjustment(final J.Binary visited) {
            final boolean leftIsMethodCall = visited.getLeft() instanceof J.MethodInvocation;
            final boolean rightIsMethodCall = visited.getRight() instanceof J.MethodInvocation;
            final boolean leftIsLiteralOne = isLiteralOne(visited.getLeft());
            final boolean rightIsLiteralOne = isLiteralOne(visited.getRight());
            return (leftIsMethodCall && rightIsLiteralOne) || (rightIsMethodCall && leftIsLiteralOne);
        }

        private boolean isLiteralOne(final Expression expr) {
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
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_NAME;
        }

        private String findEnclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : UNKNOWN_NAME;
        }
    }
}
