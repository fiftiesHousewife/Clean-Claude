package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissingExplanatoryVariableRecipe
        extends ScanningRecipe<MissingExplanatoryVariableRecipe.Accumulator> {

    private static final int CHAIN_DEPTH_THRESHOLD = 3;
    private static final int BINARY_OPERATOR_THRESHOLD = 4;
    private static final int PREVIEW_MAX_LENGTH = 60;

    public record Row(
            String className,
            String methodName,
            String expressionPreview,
            int lineNumber
    ) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Missing explanatory variable detection (G19)";
    }

    @Override
    public String getDescription() {
        return "Detects complex expressions used directly in method arguments or return statements"
                + " that should be extracted into a named variable.";
    }

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (isInitializerOfVariableDeclaration(m)) {
                    return m;
                }

                for (Expression argument : m.getArguments()) {
                    if (chainDepth(argument) >= CHAIN_DEPTH_THRESHOLD
                            && hasNonTrivialSegment(argument)) {
                        acc.rows.add(new Row(
                                findEnclosingClassName(),
                                findEnclosingMethodName(),
                                truncate(argument.printTrimmed(getCursor())),
                                -1));
                    }
                }

                return m;
            }

            /**
             * Pure getter / path-navigation chains like
             * {@code reportFile.get().getAsFile().toPath()} or
             * {@code project.getResources().getText().fromString(x)} look
             * deep but carry no hidden complexity — each segment is a
             * no-arg accessor (or final argument-taking call). The G19
             * smell is about complex expressions whose intermediate
             * results deserve names; pure navigation doesn't qualify.
             *
             * <p>Require at least one INTERMEDIATE segment (not the
             * outermost call) to take arguments before firing — that's
             * where the real "what does this compute?" complexity lives
             * (e.g. a stream {@code filter(predicate).map(transform)}).
             */
            private boolean hasNonTrivialSegment(final Expression expression) {
                if (!(expression instanceof J.MethodInvocation invocation)) {
                    return false;
                }
                Expression select = invocation.getSelect();
                while (select instanceof J.MethodInvocation inner) {
                    if (!inner.getArguments().isEmpty()
                            && !(inner.getArguments().getFirst() instanceof J.Empty)) {
                        return true;
                    }
                    select = inner.getSelect();
                }
                return false;
            }

            private boolean isInitializerOfVariableDeclaration(final J.MethodInvocation invocation) {
                final J.VariableDeclarations.NamedVariable namedVariable =
                        getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
                return namedVariable != null && namedVariable.getInitializer() == invocation;
            }

            @Override
            public J.Return visitReturn(final J.Return returnStatement, final ExecutionContext ctx) {
                final J.Return r = super.visitReturn(returnStatement, ctx);
                final Expression expression = r.getExpression();

                if (expression instanceof J.Binary && countOperators(expression) >= BINARY_OPERATOR_THRESHOLD) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            findEnclosingMethodName(),
                            truncate(expression.printTrimmed(getCursor())),
                            -1));
                }

                return r;
            }

            private int chainDepth(final Expression expression) {
                if (!(expression instanceof J.MethodInvocation invocation)) {
                    return 0;
                }
                final Expression select = invocation.getSelect();
                if (select instanceof J.MethodInvocation) {
                    return 1 + chainDepth(select);
                }
                return 1;
            }

            private int countOperators(final Expression expression) {
                if (expression instanceof J.Binary binary) {
                    return 1 + countOperators(binary.getLeft()) + countOperators(binary.getRight());
                }
                return 0;
            }

            private String truncate(final String text) {
                final String normalized = text.replaceAll("\\s+", " ");
                if (normalized.length() <= PREVIEW_MAX_LENGTH) {
                    return normalized;
                }
                return normalized.substring(0, PREVIEW_MAX_LENGTH);
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
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
