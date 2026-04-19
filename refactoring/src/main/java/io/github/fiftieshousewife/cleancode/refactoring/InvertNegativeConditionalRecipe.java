package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class InvertNegativeConditionalRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Invert negative if conditions that have an else branch";
    }

    @Override
    public String getDescription() {
        return "Rewrites `if (!cond) { A } else { B }` as `if (cond) { B } else { A }`, "
                + "so the positive case is the primary branch. Fixes G29. Conservative: only "
                + "triggers on a unary `!` expression directly at the top of the condition, "
                + "and only when an else branch is present.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.If visitIf(J.If ifStmt, ExecutionContext ctx) {
                final J.If visited = super.visitIf(ifStmt, ctx);
                final J.If.Else elseBranch = visited.getElsePart();
                if (elseBranch == null) {
                    return visited;
                }
                final Expression rawCondition = visited.getIfCondition().getTree();
                if (!(rawCondition instanceof J.Unary unary)
                        || unary.getOperator() != J.Unary.Type.Not) {
                    return visited;
                }
                if (elseBranch.getBody() instanceof J.If) {
                    return visited;
                }

                final Expression inner = unary.getExpression();
                return visited
                        .withIfCondition(visited.getIfCondition().withTree(inner))
                        .withThenPart(elseBranch.getBody())
                        .withElsePart(elseBranch.withBody(visited.getThenPart()));
            }
        };
    }
}
