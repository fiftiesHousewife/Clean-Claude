package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Collapses adjacent guard clauses that share the same body into a single
 * guard with an OR'd condition — e.g.
 *
 * <pre>
 *   if (token == null) return null;
 *   if (token.isBlank()) return null;
 * </pre>
 *
 * becomes
 *
 * <pre>
 *   if (token == null || token.isBlank()) return null;
 * </pre>
 *
 * Bodies are compared via {@link J#printTrimmed(Cursor)} using the
 * visitor's own cursor; runs of 2+ guards collapse into a single N-way
 * OR. Fixes G29.
 */
public final class CollapseSiblingGuardsRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Collapse adjacent guard clauses sharing the same body";
    }

    @Override
    public String getDescription() {
        return "Merges consecutive `if (cond) body;` statements whose body is identical into one "
                + "`if (cond1 || cond2 || ...) body;`. Only applies when none of the guards has an "
                + "else branch and every body prints the same way. Fixes G29 guard-clause noise.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block visited = (J.Block) super.visitBlock(block, ctx);
                final List<Statement> source = visited.getStatements();
                final Cursor blockCursor = getCursor();
                final List<Statement> rewritten = new ArrayList<>(source.size());
                boolean changed = false;
                int i = 0;
                while (i < source.size()) {
                    final int runEnd = findRunEnd(source, i, blockCursor);
                    if (runEnd > i) {
                        rewritten.add(mergeRun(source, i, runEnd, blockCursor));
                        changed = true;
                        i = runEnd + 1;
                    } else {
                        rewritten.add(source.get(i));
                        i++;
                    }
                }
                return changed ? visited.withStatements(rewritten) : visited;
            }
        };
    }

    private static int findRunEnd(final List<Statement> statements, final int start,
                                  final Cursor cursor) {
        if (!(statements.get(start) instanceof J.If head) || head.getElsePart() != null) {
            return start;
        }
        final String headBody = printableBody(head, cursor);
        if (headBody == null) {
            return start;
        }
        int end = start;
        for (int j = start + 1; j < statements.size(); j++) {
            if (!(statements.get(j) instanceof J.If candidate) || candidate.getElsePart() != null) {
                break;
            }
            if (!headBody.equals(printableBody(candidate, cursor))) {
                break;
            }
            end = j;
        }
        return end;
    }

    private static J.If mergeRun(final List<Statement> statements, final int start, final int end,
                                 final Cursor blockCursor) {
        final J.If first = (J.If) statements.get(start);
        final StringBuilder templateText = new StringBuilder("boolean __merged__ = #{any(boolean)}");
        final List<Object> args = new ArrayList<>();
        args.add(first.getIfCondition().getTree());
        for (int j = start + 1; j <= end; j++) {
            final J.If next = (J.If) statements.get(j);
            templateText.append(" || #{any(boolean)}");
            args.add(next.getIfCondition().getTree());
        }
        templateText.append(";");
        // Build an OR expression via a throw-away variable declaration, then extract the
        // initializer — applying a statement template at a statement coordinate is the
        // most reliable way to let JavaTemplate parse our combined predicate, even when
        // we only want the expression it contains.
        final Cursor firstCursor = new Cursor(blockCursor, first);
        final J.VariableDeclarations holder = JavaTemplate.builder(templateText.toString())
                .contextSensitive()
                .build()
                .apply(firstCursor, first.getCoordinates().replace(), args.toArray());
        final org.openrewrite.java.tree.Expression merged = holder.getVariables().get(0)
                .getInitializer().withPrefix(Space.EMPTY);
        return first.withIfCondition(first.getIfCondition().withTree(merged));
    }

    private static String printableBody(final J.If ifStmt, final Cursor cursor) {
        final Statement body = ifStmt.getThenPart();
        if (body == null) {
            return null;
        }
        return body.printTrimmed(cursor).trim();
    }
}
