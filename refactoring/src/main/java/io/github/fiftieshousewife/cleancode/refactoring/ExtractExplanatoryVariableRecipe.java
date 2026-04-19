package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtractExplanatoryVariableRecipe extends Recipe {

    private final int minChainDepth;

    @JsonCreator
    public ExtractExplanatoryVariableRecipe(@JsonProperty("minChainDepth") int minChainDepth) {
        this.minChainDepth = minChainDepth;
    }

    @Override
    public String getDisplayName() {
        return "Extract complex expressions to explanatory variables";
    }

    @Override
    public String getDescription() {
        return "Extracts method chains of depth %d+ in if-conditions to named local variables. Fixes G19/G28."
                .formatted(minChainDepth);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                final J.Block b = super.visitBlock(block, ctx);
                final List<Statement> statements = b.getStatements();
                final List<Statement> newStatements = new ArrayList<>();
                boolean changed = false;

                for (final Statement stmt : statements) {
                    if (!(stmt instanceof J.If ifStmt)) {
                        newStatements.add(stmt);
                        continue;
                    }

                    final Expression condition = ifStmt.getIfCondition().getTree();
                    final int depth = chainDepth(condition);

                    if (depth < minChainDepth) {
                        newStatements.add(stmt);
                        continue;
                    }

                    final String condText = condition.toString().trim();
                    final String varName = generateVariableName(condText);
                    final Optional<Statement> varDecl = AstFragments.parseStatement(
                            "final var %s = %s;".formatted(varName, condText));
                    if (varDecl.isEmpty()) {
                        newStatements.add(stmt);
                        continue;
                    }

                    newStatements.add(varDecl.get());

                    final J.Identifier varRef = new J.Identifier(
                            org.openrewrite.Tree.randomId(),
                            org.openrewrite.java.tree.Space.EMPTY,
                            org.openrewrite.marker.Markers.EMPTY,
                            List.of(), varName, null, null);
                    newStatements.add(ifStmt.withIfCondition(ifStmt.getIfCondition().withTree(varRef)));
                    changed = true;
                }

                return changed ? b.withStatements(newStatements) : b;
            }
        };
    }

    static int chainDepth(Expression expr) {
        if (expr instanceof J.MethodInvocation mi) {
            return mi.getSelect() != null ? 1 + chainDepth(mi.getSelect()) : 1;
        }
        if (expr instanceof J.FieldAccess fa) {
            return 1 + chainDepth(fa.getTarget());
        }
        if (expr instanceof J.Parentheses<?> p && p.getTree() instanceof Expression inner) {
            return chainDepth(inner);
        }
        if (expr instanceof J.Binary b) {
            return Math.max(chainDepth(b.getLeft()), chainDepth(b.getRight()));
        }
        return 0;
    }

    private static String generateVariableName(String conditionText) {
        if (conditionText.contains("startsWith")) {
            return "hasExpectedPrefix";
        }
        if (conditionText.contains("equals")) {
            return "isMatch";
        }
        if (conditionText.contains("contains")) {
            return "containsTarget";
        }
        return "condition";
    }
}
