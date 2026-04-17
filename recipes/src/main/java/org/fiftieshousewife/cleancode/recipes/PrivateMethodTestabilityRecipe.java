package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrivateMethodTestabilityRecipe extends ScanningRecipe<PrivateMethodTestabilityRecipe.Accumulator> {

    private static final int DEFAULT_MIN_BODY_LINES = 5;
    private static final String UNKNOWN_CLASS = "<unknown>";

    private final int minBodyLines;

    public PrivateMethodTestabilityRecipe() {
        this(DEFAULT_MIN_BODY_LINES);
    }

    public PrivateMethodTestabilityRecipe(final int minBodyLines) {
        this.minBodyLines = minBodyLines;
    }

    public record PrivateMethodTestabilityRow(
            String className,
            String methodName,
            int lineCount,
            int lineNumber
    ) {}

    public static class Accumulator {
        final List<PrivateMethodTestabilityRow> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Private method testability detection";
    }

    @Override
    public String getDescription() {
        return "Detects non-trivial private methods that should be package-private for testability.";
    }

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new PrivateMethodScanner(acc, minBodyLines);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<PrivateMethodTestabilityRow> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class PrivateMethodScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;
        private final int minBodyLines;

        PrivateMethodScanner(final Accumulator acc, final int minBodyLines) {
            this.acc = acc;
            this.minBodyLines = minBodyLines;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                          final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            if (shouldSkip(visited)) {
                return visited;
            }
            final int bodyLineCount = bodyStatementCount(visited);
            if (bodyLineCount <= minBodyLines) {
                return visited;
            }
            acc.rows.add(toRow(visited, bodyLineCount));
            return visited;
        }

        private boolean shouldSkip(final J.MethodDeclaration method) {
            return !isPrivate(method)
                    || isConstructor(method)
                    || isInsideAnonymousClassOrLambda();
        }

        private int bodyStatementCount(final J.MethodDeclaration method) {
            final J.Block body = method.getBody();
            return body != null ? body.getStatements().size() : -1;
        }

        private PrivateMethodTestabilityRow toRow(final J.MethodDeclaration method, final int bodyLineCount) {
            return new PrivateMethodTestabilityRow(
                    findEnclosingClassName(),
                    method.getSimpleName(),
                    bodyLineCount,
                    -1
            );
        }

        private boolean isPrivate(final J.MethodDeclaration method) {
            return method.getModifiers().stream()
                    .anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
        }

        private boolean isConstructor(final J.MethodDeclaration method) {
            return method.getMethodType() != null && method.getMethodType().isConstructor();
        }

        private boolean isInsideAnonymousClassOrLambda() {
            return isInsideAnonymousClass() || isInsideLambda();
        }

        private boolean isInsideAnonymousClass() {
            return getCursor().firstEnclosing(J.NewClass.class) != null;
        }

        private boolean isInsideLambda() {
            return getCursor().firstEnclosing(J.Lambda.class) != null;
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS;
        }
    }
}
