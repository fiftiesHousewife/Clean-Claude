package io.github.fiftieshousewife.cleancode.refactoring;

import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import io.github.fiftieshousewife.cleancode.refactoring.support.Statements;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class RemoveNestedTernaryRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace nested ternary with if/else";
    }

    @Override
    public String getDescription() {
        return "Replaces nested ternary expressions in return statements with if/else chains. Fixes G16.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                final J.Block b = super.visitBlock(block, ctx);
                return Statements.rebuild(b, stmt -> {
                    if (!(stmt instanceof J.Return ret) || ret.getExpression() == null
                            || !(ret.getExpression() instanceof J.Ternary ternary)
                            || !hasNestedTernary(ternary)) {
                        return List.of(stmt);
                    }
                    final String ifElse = ternaryToIfElse(ternary, "result");
                    final List<Statement> parsed = AstFragments.parseStatements(
                            "Object result; %s return result;".formatted(ifElse));
                    if (parsed.isEmpty()) {
                        return List.of(stmt);
                    }
                    return parsed.stream()
                            .map(s -> (Statement) s.withPrefix(stmt.getPrefix()))
                            .toList();
                });
            }
        };
    }

    private static boolean hasNestedTernary(J.Ternary ternary) {
        return ternary.getTruePart() instanceof J.Ternary
                || ternary.getFalsePart() instanceof J.Ternary;
    }

    private static String ternaryToIfElse(J.Ternary ternary, String varName) {
        final String condition = ternary.getCondition().toString().trim();
        final var truePart = ternary.getTruePart();
        final var falsePart = ternary.getFalsePart();

        final StringBuilder sb = new StringBuilder();
        sb.append("if (").append(condition).append(") { ")
                .append(varName).append(" = ").append(truePart.toString().trim()).append("; }");

        if (falsePart instanceof J.Ternary nested) {
            sb.append(" else ").append(ternaryToIfElse(nested, varName));
        } else {
            sb.append(" else { ").append(varName).append(" = ")
                    .append(falsePart.toString().trim()).append("; }");
        }

        return sb.toString();
    }
}
