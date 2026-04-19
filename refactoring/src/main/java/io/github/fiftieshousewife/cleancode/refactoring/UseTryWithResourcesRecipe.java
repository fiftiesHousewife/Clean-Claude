package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D10 remediation: rewrite manual {@code close()}-in-finally idioms to
 * try-with-resources. Covers two shapes:
 * <pre>
 *     Foo r = ...;
 *     try { ... } finally { r.close(); }
 * </pre>
 * and the null-guarded variant:
 * <pre>
 *     Foo r = ...;
 *     try { ... } finally { if (r != null) r.close(); }
 * </pre>
 * <p>Preserves any catch clauses on the original try — only the
 * finally that does nothing but close the lifted resource is removed.
 * Leaves the code alone when the finally does anything else, when the
 * close targets a different variable, or when the preceding declaration
 * has no initializer (nothing to hoist into the try head).
 */
public final class UseTryWithResourcesRecipe extends Recipe {

    private static final String CLOSE = "close";

    @Override
    public String getDisplayName() {
        return "Use try-with-resources";
    }

    @Override
    public String getDescription() {
        return "Rewrites a variable declaration followed by a try/finally that only closes "
                + "that variable (optionally guarded by `if (r != null)`) into a "
                + "try-with-resources. Leaves busy finallies and mismatched closes alone.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block visited = super.visitBlock(block, ctx);
                final List<Statement> statements = visited.getStatements();
                final List<Statement> rewritten = new ArrayList<>(statements.size());
                int i = 0;
                boolean changed = false;
                while (i < statements.size()) {
                    final Statement current = statements.get(i);
                    final Statement next = i + 1 < statements.size() ? statements.get(i + 1) : null;
                    if (next != null && isCandidatePair(current, next)) {
                        rewritten.add(buildTryWithResources((J.VariableDeclarations) current, (J.Try) next));
                        i += 2;
                        changed = true;
                    } else {
                        rewritten.add(current);
                        i += 1;
                    }
                }
                return changed ? visited.withStatements(rewritten) : visited;
            }
        };
    }

    private static boolean isCandidatePair(final Statement decl, final Statement tryStmt) {
        if (!(decl instanceof J.VariableDeclarations vd) || !(tryStmt instanceof J.Try tryBlock)) {
            return false;
        }
        if (vd.getVariables().size() != 1) {
            return false;
        }
        final J.VariableDeclarations.NamedVariable variable = vd.getVariables().get(0);
        if (variable.getInitializer() == null) {
            return false;
        }
        if (tryBlock.getFinally() == null) {
            return false;
        }
        if (tryBlock.getResources() != null && !tryBlock.getResources().isEmpty()) {
            return false;
        }
        return finallyClosesOnly(tryBlock.getFinally(), variable.getSimpleName());
    }

    private static boolean finallyClosesOnly(final J.Block finallyBlock, final String targetName) {
        final List<Statement> stmts = finallyBlock.getStatements();
        if (stmts.size() != 1) {
            return false;
        }
        final Statement only = stmts.get(0);
        if (isCloseCall(only, targetName)) {
            return true;
        }
        if (only instanceof J.If guard) {
            return isNullGuard(guard.getIfCondition().getTree(), targetName)
                    && guard.getElsePart() == null
                    && guardBodyIsCloseCall(guard.getThenPart(), targetName);
        }
        return false;
    }

    private static boolean isNullGuard(final J condition, final String targetName) {
        if (!(condition instanceof J.Binary binary)) {
            return false;
        }
        if (binary.getOperator() != J.Binary.Type.NotEqual) {
            return false;
        }
        final boolean leftIsTarget = binary.getLeft() instanceof J.Identifier id
                && targetName.equals(id.getSimpleName());
        final boolean rightIsNull = binary.getRight() instanceof J.Literal lit && lit.getValue() == null;
        final boolean rightIsTarget = binary.getRight() instanceof J.Identifier id
                && targetName.equals(id.getSimpleName());
        final boolean leftIsNull = binary.getLeft() instanceof J.Literal lit && lit.getValue() == null;
        return (leftIsTarget && rightIsNull) || (rightIsTarget && leftIsNull);
    }

    private static boolean guardBodyIsCloseCall(final Statement body, final String targetName) {
        if (body instanceof J.Block block) {
            return block.getStatements().size() == 1
                    && isCloseCall(block.getStatements().get(0), targetName);
        }
        return isCloseCall(body, targetName);
    }

    private static boolean isCloseCall(final Statement stmt, final String targetName) {
        if (!(stmt instanceof J.MethodInvocation invocation)) {
            return false;
        }
        if (!CLOSE.equals(invocation.getSimpleName()) || !invocation.getArguments().stream()
                .allMatch(arg -> arg instanceof J.Empty)) {
            return false;
        }
        return invocation.getSelect() instanceof J.Identifier id
                && targetName.equals(id.getSimpleName());
    }

    private static J.Try buildTryWithResources(final J.VariableDeclarations decl, final J.Try originalTry) {
        final J.VariableDeclarations resourceDecl = decl
                .withModifiers(Collections.emptyList())
                .withPrefix(Space.EMPTY);
        final J.Try.Resource resource = new J.Try.Resource(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                resourceDecl,
                false);
        final JContainer<J.Try.Resource> resources = JContainer.build(
                Space.format(" "),
                List.of(JRightPadded.build(resource)),
                Markers.EMPTY);
        return originalTry
                .getPadding()
                .withResources(resources)
                .withFinally(null)
                .withPrefix(decl.getPrefix());
    }
}
