package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NullDensityRecipe extends ScanningRecipe<NullDensityRecipe.Accumulator> {

    private static final int DEFAULT_THRESHOLD = 3;

    public record Row(String className, String methodName, int nullCheckCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Null density detection (Ch7.2)";
    }

    @Override
    public String getDescription() {
        return "Detects methods with excessive null checks (>= threshold).";
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
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                final int count = countNullChecks(m);

                if (count >= DEFAULT_THRESHOLD) {
                    final String className = findEnclosingClassName();
                    acc.rows.add(new Row(className, m.getSimpleName(), count, -1));
                }

                return m;
            }

            private int countNullChecks(J.MethodDeclaration method) {
                final int[] count = {0};
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Binary visitBinary(J.Binary binary, Integer unused) {
                        final J.Binary b = super.visitBinary(binary, unused);
                        if (isNullComparison(b)) {
                            count[0]++;
                        }
                        return b;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, Integer unused) {
                        final J.MethodInvocation mi = super.visitMethodInvocation(invocation, unused);
                        if (isObjectsNullCheck(mi)) {
                            count[0]++;
                        }
                        return mi;
                    }
                }.visit(method, 0);
                return count[0];
            }

            private boolean isNullComparison(J.Binary binary) {
                return (binary.getOperator() == J.Binary.Type.Equal
                        || binary.getOperator() == J.Binary.Type.NotEqual)
                        && (isNullLiteral(binary.getLeft()) || isNullLiteral(binary.getRight()));
            }

            private boolean isNullLiteral(org.openrewrite.java.tree.Expression expr) {
                return expr instanceof J.Literal literal && literal.getType() == org.openrewrite.java.tree.JavaType.Primitive.Null;
            }

            private boolean isObjectsNullCheck(J.MethodInvocation invocation) {
                final String name = invocation.getSimpleName();
                return "isNull".equals(name) || "nonNull".equals(name) || "requireNonNull".equals(name);
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
