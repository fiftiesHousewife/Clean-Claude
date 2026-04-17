package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShortVariableNameRecipe extends ScanningRecipe<ShortVariableNameRecipe.Accumulator> {

    private static final int DEFAULT_MIN_NAME_LENGTH = 2;
    private static final String UNKNOWN = "<unknown>";
    private static final String UNKNOWN_TYPE = "unknown";
    private static final String LOCAL_VARIABLE_CONTEXT_PREFIX = "local variable of type ";
    private static final String PARAMETER_CONTEXT = "parameter";
    private static final int MISSING_LINE_NUMBER = -1;

    private final int minNameLength;

    public ShortVariableNameRecipe() {
        this(DEFAULT_MIN_NAME_LENGTH);
    }

    public ShortVariableNameRecipe(final int minNameLength) {
        this.minNameLength = minNameLength;
    }

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
        return new ShortVariableNameVisitor(acc, minNameLength);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class ShortVariableNameVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;
        private final int minNameLength;

        ShortVariableNameVisitor(final Accumulator acc, final int minNameLength) {
            this.acc = acc;
            this.minNameLength = minNameLength;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                final J.VariableDeclarations varDecls, final ExecutionContext ctx) {
            final J.VariableDeclarations visited = super.visitVariableDeclarations(varDecls, ctx);
            if (isInExemptScope()) {
                return visited;
            }
            visited.getVariables().forEach(variable -> recordIfShort(variable, visited));
            return visited;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            visited.getParameters().stream()
                    .filter(p -> p instanceof J.VariableDeclarations)
                    .map(p -> (J.VariableDeclarations) p)
                    .forEach(paramDecl -> recordShortParameters(paramDecl, visited.getSimpleName()));
            return visited;
        }

        private void recordIfShort(final J.VariableDeclarations.NamedVariable variable,
                                   final J.VariableDeclarations varDecls) {
            final String name = variable.getSimpleName();
            if (isAcceptableName(name)) {
                return;
            }
            acc.rows.add(new Row(
                    findEnclosingClassName(),
                    findEnclosingMethodName(),
                    name,
                    describeContext(varDecls),
                    MISSING_LINE_NUMBER));
        }

        private void recordShortParameters(final J.VariableDeclarations paramDecl, final String methodName) {
            paramDecl.getVariables().forEach(param -> {
                final String name = param.getSimpleName();
                if (isAcceptableName(name)) {
                    return;
                }
                acc.rows.add(new Row(
                        findEnclosingClassName(),
                        methodName,
                        name,
                        PARAMETER_CONTEXT,
                        MISSING_LINE_NUMBER));
            });
        }

        private boolean isAcceptableName(final String name) {
            return name.length() >= minNameLength || AllowedShortNames.contains(name);
        }

        private boolean isInExemptScope() {
            return isInsideForLoop() || isInsideLambda() || isMethodParameter();
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

        private String describeContext(final J.VariableDeclarations varDecls) {
            final String typeStr = varDecls.getType() != null
                    ? varDecls.getType().toString()
                    : UNKNOWN_TYPE;
            return LOCAL_VARIABLE_CONTEXT_PREFIX + typeStr;
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN;
        }

        private String findEnclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : UNKNOWN;
        }
    }
}
