package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NegativeConditionalRecipe extends ScanningRecipe<NegativeConditionalRecipe.Accumulator> {

    private static final Set<String> NEGATIVE_PREFIXES = Set.of("isNot", "isNo", "hasNot", "hasNo");

    public record Row(String className, String methodName, String expression, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Negative conditional detection (G29)";
    }

    @Override
    public String getDescription() {
        return "Detects double negation: negated method calls where the method name starts with isNot/hasNot.";
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
            public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                final J.Unary u = super.visitUnary(unary, ctx);

                if (u.getOperator() != J.Unary.Type.Not) {
                    return u;
                }

                if (u.getExpression() instanceof J.MethodInvocation invocation) {
                    final String name = invocation.getSimpleName();
                    if (hasNegativePrefix(name)) {
                        acc.rows.add(new Row(
                                findEnclosingClassName(),
                                findEnclosingMethodName(),
                                "!" + name + "()",
                                -1));
                    }
                }

                return u;
            }

            private boolean hasNegativePrefix(String methodName) {
                return NEGATIVE_PREFIXES.stream().anyMatch(methodName::startsWith);
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
