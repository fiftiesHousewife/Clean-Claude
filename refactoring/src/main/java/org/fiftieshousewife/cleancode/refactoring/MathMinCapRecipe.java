package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

/**
 * Replaces the cap / floor idiom
 *
 * <pre>
 *   if (v &gt; CAP)   { v = CAP; }       // → v = Math.min(v, CAP);
 *   if (v &lt; FLOOR) { v = FLOOR; }     // → v = Math.max(v, FLOOR);
 * </pre>
 *
 * with a {@code Math.min} / {@code Math.max} call. Only fires when the
 * variable on the left of the comparison is the same as the assignment
 * target, and the assigned value matches the comparison's right-hand
 * side exactly (textually). Leaves anything with an else branch or
 * additional body statements alone.
 */
public final class MathMinCapRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace cap/floor if-assignment with Math.min / Math.max";
    }

    @Override
    public String getDescription() {
        return "Rewrites `if (v > CAP) v = CAP;` as `v = Math.min(v, CAP);` and the symmetric "
                + "floor case as `Math.max`. Only applies when the compared identifier, the "
                + "assignment target, and the assigned value all align. Fixes the G30/G33 "
                + "pattern of encoding a boundary clamp as branching control flow.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitIf(final J.If ifStmt, final ExecutionContext ctx) {
                final J visited = super.visitIf(ifStmt, ctx);
                if (!(visited instanceof J.If asIf) || asIf.getElsePart() != null) {
                    return visited;
                }
                if (!(asIf.getIfCondition().getTree() instanceof J.Binary comparison)) {
                    return visited;
                }
                final String mathMethod = chooseMathMethod(comparison.getOperator());
                if (mathMethod == null) {
                    return visited;
                }
                final J.Assignment singleAssignment = singleAssignmentIn(asIf.getThenPart());
                if (singleAssignment == null) {
                    return visited;
                }
                if (!isClampOf(comparison, singleAssignment, getCursor())) {
                    return visited;
                }
                final JavaTemplate template = JavaTemplate.builder(
                                "#{any()} = Math." + mathMethod + "(#{any()}, #{any()});")
                        .contextSensitive()
                        .build();
                final Expression lhs = comparison.getLeft();
                final Expression rhs = comparison.getRight();
                return template.apply(getCursor(), asIf.getCoordinates().replace(), lhs, lhs, rhs);
            }
        };
    }

    private static String chooseMathMethod(final J.Binary.Type operator) {
        return switch (operator) {
            case GreaterThan, GreaterThanOrEqual -> "min";
            case LessThan, LessThanOrEqual -> "max";
            default -> null;
        };
    }

    private static J.Assignment singleAssignmentIn(final Statement body) {
        if (body instanceof J.Block block) {
            final List<Statement> statements = block.getStatements();
            if (statements.size() != 1) {
                return null;
            }
            return asAssignment(statements.get(0));
        }
        return asAssignment(body);
    }

    private static J.Assignment asAssignment(final Statement statement) {
        return statement instanceof J.Assignment assignment ? assignment : null;
    }

    private static boolean isClampOf(final J.Binary comparison, final J.Assignment assignment,
                                     final Cursor cursor) {
        if (!(comparison.getLeft() instanceof J.Identifier comparedIdent)) {
            return false;
        }
        if (!(assignment.getVariable() instanceof J.Identifier targetIdent)) {
            return false;
        }
        if (!comparedIdent.getSimpleName().equals(targetIdent.getSimpleName())) {
            return false;
        }
        return printableTrimmed(comparison.getRight(), cursor)
                .equals(printableTrimmed(assignment.getAssignment(), cursor));
    }

    private static String printableTrimmed(final Expression e, final Cursor cursor) {
        return e.printTrimmed(cursor).trim();
    }
}
