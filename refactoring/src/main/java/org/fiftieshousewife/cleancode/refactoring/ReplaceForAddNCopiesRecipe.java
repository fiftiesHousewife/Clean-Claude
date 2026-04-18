package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replaces the idiom
 *
 * <pre>
 *   for (int i = 0; i &lt; N; i++) {
 *       list.add(value);
 *   }
 * </pre>
 *
 * with
 *
 * <pre>
 *   list.addAll(Collections.nCopies(N, value));
 * </pre>
 *
 * Preconditions (all must hold):
 * <ul>
 *   <li>init is exactly {@code int <name> = 0};</li>
 *   <li>condition is {@code <name> &lt; <expr>};</li>
 *   <li>update is {@code <name>++} or {@code ++<name>};</li>
 *   <li>body is a single {@code <list>.add(<value>);} call;</li>
 *   <li>neither {@code <list>} nor {@code <value>} references the counter.</li>
 * </ul>
 *
 * Fixes the G30 "imperative retry/repeat loop" pattern.
 */
public final class ReplaceForAddNCopiesRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace `for (i=0; i<N; i++) list.add(v)` with `list.addAll(Collections.nCopies(N, v))`";
    }

    @Override
    public String getDescription() {
        return "Detects for-loops that append the same value N times to a list and rewrites "
                + "them as a single `addAll(Collections.nCopies(N, value))` call. Leaves loops "
                + "that index into the body (use `i`) or do anything besides a single add alone.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitForLoop(final J.ForLoop forLoop, final ExecutionContext ctx) {
                final J visited = super.visitForLoop(forLoop, ctx);
                if (!(visited instanceof J.ForLoop loop)) {
                    return visited;
                }
                final CounterBinding counter = counterBinding(loop);
                if (counter == null) {
                    return visited;
                }
                final J.MethodInvocation addCall = singleAddCall(loop.getBody());
                if (addCall == null) {
                    return visited;
                }
                if (referencesCounter(addCall, counter.name())) {
                    return visited;
                }
                final Expression listExpr = addCall.getSelect();
                final Expression valueExpr = addCall.getArguments().get(0);
                if (listExpr == null) {
                    return visited;
                }
                maybeAddImport("java.util.Collections");
                return JavaTemplate.builder("#{any()}.addAll(Collections.nCopies(#{any(int)}, #{any()}));")
                        .contextSensitive()
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), loop.getCoordinates().replace(),
                                listExpr, counter.limit(), valueExpr);
            }
        };
    }

    private record CounterBinding(String name, Expression limit) {}

    private static CounterBinding counterBinding(final J.ForLoop loop) {
        final J.ForLoop.Control control = loop.getControl();
        if (control.getInit().size() != 1
                || !(control.getInit().get(0) instanceof J.VariableDeclarations initDecl)) {
            return null;
        }
        if (initDecl.getVariables().size() != 1) {
            return null;
        }
        final J.VariableDeclarations.NamedVariable named = initDecl.getVariables().get(0);
        if (!(named.getInitializer() instanceof J.Literal lit) || !Integer.valueOf(0).equals(lit.getValue())) {
            return null;
        }
        if (!(control.getCondition() instanceof J.Binary condition)
                || condition.getOperator() != J.Binary.Type.LessThan
                || !(condition.getLeft() instanceof J.Identifier counterRef)
                || !counterRef.getSimpleName().equals(named.getSimpleName())) {
            return null;
        }
        if (!isIncrementOf(control.getUpdate(), named.getSimpleName())) {
            return null;
        }
        return new CounterBinding(named.getSimpleName(), condition.getRight());
    }

    private static boolean isIncrementOf(final List<Statement> updates, final String name) {
        if (updates.size() != 1 || !(updates.get(0) instanceof J.Unary unary)) {
            return false;
        }
        if (unary.getOperator() != J.Unary.Type.PostIncrement
                && unary.getOperator() != J.Unary.Type.PreIncrement) {
            return false;
        }
        return unary.getExpression() instanceof J.Identifier id
                && id.getSimpleName().equals(name);
    }

    private static J.MethodInvocation singleAddCall(final Statement body) {
        if (!(body instanceof J.Block block)) {
            return null;
        }
        if (block.getStatements().size() != 1) {
            return null;
        }
        if (!(block.getStatements().get(0) instanceof J.MethodInvocation call)) {
            return null;
        }
        if (!"add".equals(call.getSimpleName()) || call.getArguments().size() != 1) {
            return null;
        }
        if (call.getSelect() == null) {
            return null;
        }
        return call;
    }

    private static boolean referencesCounter(final J.MethodInvocation call, final String counterName) {
        final AtomicBoolean found = new AtomicBoolean(false);
        new JavaVisitor<AtomicBoolean>() {
            @Override
            public J visitIdentifier(final J.Identifier id, final AtomicBoolean flag) {
                if (counterName.equals(id.getSimpleName())) {
                    flag.set(true);
                }
                return super.visitIdentifier(id, flag);
            }
        }.visit(call, found);
        return found.get();
    }
}
