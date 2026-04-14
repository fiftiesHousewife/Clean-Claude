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

    private static final int MIN_BODY_LINES = 5;

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
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                              final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (!isPrivate(m) || isConstructor(m) || isInsideAnonymousClassOrLambda()) {
                    return m;
                }

                final J.Block body = m.getBody();
                if (body == null) {
                    return m;
                }

                final int bodyLineCount = body.getStatements().size();
                if (bodyLineCount <= MIN_BODY_LINES) {
                    return m;
                }

                final String className = findEnclosingClassName();

                acc.rows.add(new PrivateMethodTestabilityRow(
                        className,
                        m.getSimpleName(),
                        bodyLineCount,
                        -1
                ));

                return m;
            }

            private boolean isPrivate(final J.MethodDeclaration method) {
                return method.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
            }

            private boolean isConstructor(final J.MethodDeclaration method) {
                return method.getMethodType() != null && method.getMethodType().isConstructor();
            }

            private boolean isInsideAnonymousClassOrLambda() {
                return getCursor().firstEnclosing(J.NewClass.class) != null
                        || getCursor().firstEnclosing(J.Lambda.class) != null;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<PrivateMethodTestabilityRow> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
