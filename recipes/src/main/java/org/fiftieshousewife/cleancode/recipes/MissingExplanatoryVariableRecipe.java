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

public class MissingExplanatoryVariableRecipe
        extends ScanningRecipe<MissingExplanatoryVariableRecipe.Accumulator> {

    private static final int CHAIN_DEPTH_THRESHOLD = 3;
    private static final int BINARY_OPERATOR_THRESHOLD = 4;
    private static final int PREVIEW_MAX_LENGTH = 60;
    private static final String UNKNOWN_ENCLOSING_NAME = "<unknown>";
    private static final int UNKNOWN_LINE_NUMBER = -1;

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
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new Scanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class Scanner extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        Scanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
            final J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
            visited.getArguments().stream()
                    .filter(argument -> chainDepth(argument) >= CHAIN_DEPTH_THRESHOLD)
                    .forEach(argument -> acc.rows.add(rowFor(argument)));
            return visited;
        }

        @Override
        public J.Return visitReturn(final J.Return returnStatement, final ExecutionContext ctx) {
            final J.Return visited = super.visitReturn(returnStatement, ctx);
            final Expression expression = visited.getExpression();
            if (expression instanceof J.Binary && countOperators(expression) >= BINARY_OPERATOR_THRESHOLD) {
                acc.rows.add(rowFor(expression));
            }
            return visited;
        }

        private Row rowFor(final Expression expression) {
            return new Row(
                    findEnclosingClassName(),
                    findEnclosingMethodName(),
                    truncate(expression.printTrimmed(getCursor())),
                    UNKNOWN_LINE_NUMBER);
        }

        int chainDepth(final Expression expression) {
            if (!(expression instanceof J.MethodInvocation invocation)) {
                return 0;
            }
            final Expression select = invocation.getSelect();
            final int selectDepth = select instanceof J.MethodInvocation ? chainDepth(select) : 0;
            final int thisInvocation = 1;
            return thisInvocation + selectDepth;
        }

        int countOperators(final Expression expression) {
            if (!(expression instanceof J.Binary binary)) {
                return 0;
            }
            final int leftOperators = countOperators(binary.getLeft());
            final int rightOperators = countOperators(binary.getRight());
            final int thisOperator = 1;
            return thisOperator + leftOperators + rightOperators;
        }

        String truncate(final String text) {
            final String normalized = text.replaceAll("\\s+", " ");
            if (normalized.length() <= PREVIEW_MAX_LENGTH) {
                return normalized;
            }
            return normalized.substring(0, PREVIEW_MAX_LENGTH);
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_ENCLOSING_NAME;
        }

        private String findEnclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : UNKNOWN_ENCLOSING_NAME;
        }
    }
}
