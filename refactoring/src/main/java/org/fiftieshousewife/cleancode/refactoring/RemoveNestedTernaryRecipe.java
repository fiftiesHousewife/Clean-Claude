package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
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
                final List<Statement> statements = b.getStatements();
                final List<Statement> newStatements = new ArrayList<>();
                boolean changed = false;

                for (final Statement stmt : statements) {
                    if (!(stmt instanceof J.Return ret) || ret.getExpression() == null
                            || !(ret.getExpression() instanceof J.Ternary ternary)
                            || !hasNestedTernary(ternary)) {
                        newStatements.add(stmt);
                        continue;
                    }

                    final String ifElse = ternaryToIfElse(ternary, "result");
                    final String wrapper = "class _T { Object _m() { Object result; %s return result; } }"
                            .formatted(ifElse);

                    final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                            .logCompilationWarningsAndErrors(false)
                            .build().parse(wrapper).toList();

                    if (parsed.isEmpty()) {
                        newStatements.add(stmt);
                        continue;
                    }

                    final J.MethodDeclaration method = (J.MethodDeclaration)
                            ((J.CompilationUnit) parsed.getFirst())
                                    .getClasses().getFirst().getBody().getStatements().getFirst();
                    method.getBody().getStatements()
                            .forEach(s -> newStatements.add(s.withPrefix(stmt.getPrefix())));
                    changed = true;
                }

                return changed ? b.withStatements(newStatements) : b;
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
