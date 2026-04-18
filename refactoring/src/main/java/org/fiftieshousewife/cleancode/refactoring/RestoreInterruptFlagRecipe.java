package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ch7.1 remediation: when a {@code catch (InterruptedException …)} block
 * doesn't re-assert the thread's interrupt flag (by calling
 * {@code Thread.currentThread().interrupt()}) or rethrow the exception,
 * insert {@code Thread.currentThread().interrupt();} at the top of the
 * catch body so interrupt semantics propagate.
 *
 * <p>Leaves the catch alone if:
 * <ul>
 *   <li>the body already contains that exact call, or</li>
 *   <li>the body rethrows the caught exception (an {@code InterruptedException}
 *       rethrow preserves interrupt semantics by the caller's contract).</li>
 * </ul>
 */
public final class RestoreInterruptFlagRecipe extends Recipe {

    private static final String INTERRUPTED_EXCEPTION_FQN = "java.lang.InterruptedException";
    private static final String RESTORE_CALL = "Thread.currentThread().interrupt();";

    @Override
    public String getDisplayName() {
        return "Restore the interrupt flag in InterruptedException catch blocks";
    }

    @Override
    public String getDescription() {
        return "Inserts `Thread.currentThread().interrupt();` at the start of any "
                + "`catch (InterruptedException …)` block that neither re-interrupts nor "
                + "rethrows the caught exception. Fixes the Ch7.1 interrupt-swallow pattern.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Try.Catch visitCatch(final J.Try.Catch catchClause, final ExecutionContext ctx) {
                final J.Try.Catch c = super.visitCatch(catchClause, ctx);
                if (!catchesInterruptedException(c)) {
                    return c;
                }
                if (alreadyRestoresFlag(c) || rethrowsCaught(c)) {
                    return c;
                }
                return insertRestoreCall(c);
            }

            private J.Try.Catch insertRestoreCall(final J.Try.Catch c) {
                final J.Block body = c.getBody();
                final JavaTemplate template = JavaTemplate.builder(RESTORE_CALL)
                        .contextSensitive()
                        .build();
                final J.Block updated;
                if (body.getStatements().isEmpty()) {
                    updated = template.apply(
                            new org.openrewrite.Cursor(getCursor(), body),
                            body.getCoordinates().lastStatement());
                } else {
                    updated = template.apply(
                            new org.openrewrite.Cursor(getCursor(), body),
                            body.getStatements().get(0).getCoordinates().before());
                }
                return c.withBody(updated);
            }
        };
    }

    private static boolean catchesInterruptedException(final J.Try.Catch c) {
        final J.VariableDeclarations param = c.getParameter().getTree();
        if (param.getTypeExpression() == null) {
            return false;
        }
        final JavaType type = param.getTypeExpression().getType();
        if (type == null) {
            return false;
        }
        return INTERRUPTED_EXCEPTION_FQN.equals(type.toString());
    }

    private static boolean alreadyRestoresFlag(final J.Try.Catch c) {
        final AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation m,
                                                            final AtomicBoolean flag) {
                if (!flag.get() && isRestoreCall(m)) {
                    flag.set(true);
                }
                return super.visitMethodInvocation(m, flag);
            }
        }.visit(c.getBody(), found);
        return found.get();
    }

    private static boolean isRestoreCall(final J.MethodInvocation m) {
        if (!"interrupt".equals(m.getSimpleName()) || !m.getArguments().stream()
                .allMatch(a -> a instanceof J.Empty)) {
            return false;
        }
        if (!(m.getSelect() instanceof J.MethodInvocation inner)) {
            return false;
        }
        return "currentThread".equals(inner.getSimpleName())
                && inner.getSelect() instanceof J.Identifier id
                && "Thread".equals(id.getSimpleName());
    }

    private static boolean rethrowsCaught(final J.Try.Catch c) {
        final String paramName = c.getParameter().getTree().getVariables().get(0).getSimpleName();
        final AtomicBoolean rethrows = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Throw visitThrow(final J.Throw t, final AtomicBoolean flag) {
                if (!flag.get() && t.getException() instanceof J.Identifier id
                        && paramName.equals(id.getSimpleName())) {
                    flag.set(true);
                }
                return super.visitThrow(t, flag);
            }
        }.visit(c.getBody(), rethrows);
        return rethrows.get();
    }
}
