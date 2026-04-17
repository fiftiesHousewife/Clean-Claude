package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

final class MultipleAssertScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final String UNKNOWN_CLASS_NAME = "<unknown>";

    private final MultipleAssertRecipe.Accumulator acc;
    private final int threshold;

    MultipleAssertScanner(final MultipleAssertRecipe.Accumulator acc, final int threshold) {
        this.acc = acc;
        this.threshold = threshold;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
        final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        if (!isTestMethod(m) || m.getBody() == null) {
            return m;
        }
        final int consecutive = longestAssertRun(m.getBody().getStatements());
        if (consecutive >= threshold) {
            acc.rows.add(new MultipleAssertRecipe.Row(enclosingClassName(), m.getSimpleName(), consecutive));
        }
        return m;
    }

    private static boolean isTestMethod(final J.MethodDeclaration method) {
        return method.getLeadingAnnotations().stream()
                .anyMatch(ann -> "Test".equals(ann.getSimpleName()));
    }

    private static int longestAssertRun(final List<Statement> statements) {
        int maxRun = 0;
        int currentRun = 0;
        for (final Statement stmt : statements) {
            currentRun = isAssertCall(stmt) ? currentRun + 1 : 0;
            maxRun = Math.max(maxRun, currentRun);
        }
        return maxRun;
    }

    private static boolean isAssertCall(final Statement stmt) {
        return stmt instanceof J.MethodInvocation invocation && isAssertInvocation(invocation);
    }

    private static boolean isAssertInvocation(final J.MethodInvocation invocation) {
        if (AssertPrefixes.matches(invocation.getSimpleName())) {
            return true;
        }
        return invocation.getSelect() instanceof J.MethodInvocation parent && isAssertInvocation(parent);
    }

    private String enclosingClassName() {
        final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS_NAME;
    }
}
