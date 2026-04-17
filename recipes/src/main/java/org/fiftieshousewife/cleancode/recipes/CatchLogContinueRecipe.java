package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CatchLogContinueRecipe extends ScanningRecipe<CatchLogContinueRecipe.Accumulator> {

    private static final Set<String> LOGGER_METHOD_NAMES = Set.of(
            "trace", "debug", "info", "warn", "error", "fatal", "log",
            "printStackTrace", "println");

    public record Row(String className, String methodName, String exceptionType, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Catch-log-continue detection (Ch7.1)";
    }

    @Override
    public String getDescription() {
        return "Detects catch blocks whose body contains only logging or is empty.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new CatchLogContinueVisitor(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class CatchLogContinueVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        CatchLogContinueVisitor(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.Try visitTry(final J.Try tryStatement, final ExecutionContext ctx) {
            final J.Try t = super.visitTry(tryStatement, ctx);

            for (final J.Try.Catch catchClause : t.getCatches()) {
                if (isCatchLogContinue(catchClause)) {
                    final String className = findEnclosingClassName();
                    final String methodName = findEnclosingMethodName();
                    final String exceptionType = resolveExceptionType(catchClause);

                    acc.rows.add(new Row(className, methodName, exceptionType, -1));
                }
            }

            return t;
        }

        private String resolveExceptionType(final J.Try.Catch catchClause) {
            if (catchClause.getParameter().getTree().getTypeAsFullyQualified() != null) {
                return catchClause.getParameter().getTree().getTypeAsFullyQualified().getClassName();
            }
            if (catchClause.getParameter().getTree().getType() != null) {
                return catchClause.getParameter().getTree().getType().toString();
            }
            return "Exception";
        }

        private boolean isCatchLogContinue(final J.Try.Catch catchClause) {
            final List<Statement> statements = catchClause.getBody().getStatements();

            if (statements.isEmpty()) {
                return true;
            }

            if (containsThrow(statements)) {
                return false;
            }

            return statements.stream().allMatch(this::isLoggingStatement);
        }

        private boolean containsThrow(final List<Statement> statements) {
            return statements.stream().anyMatch(s -> s instanceof J.Throw);
        }

        private boolean isLoggingStatement(final Statement statement) {
            if (statement instanceof J.MethodInvocation invocation) {
                return isLoggerCall(invocation);
            }
            return false;
        }

        private boolean isLoggerCall(final J.MethodInvocation invocation) {
            final String methodName = invocation.getSimpleName();
            return LOGGER_METHOD_NAMES.contains(methodName);
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
        }

        private String findEnclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : "<unknown>";
        }
    }
}
