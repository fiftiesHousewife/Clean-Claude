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

    private static final String RESULT_VAR = "result";
    private static final String ASSIGNMENT = " = ";
    private static final String BLOCK_END = "; }";
    private static final String WRAPPER_TEMPLATE =
            "class _T { Object _m() { Object result; %s return result; } }";

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
        return new NestedTernaryVisitor();
    }

    private static final class NestedTernaryVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            final J.Block visitedBlock = super.visitBlock(block, ctx);
            final List<Statement> rewritten = rewriteStatements(visitedBlock.getStatements());
            if (rewritten == null) {
                return visitedBlock;
            }
            return visitedBlock.withStatements(rewritten);
        }

        private List<Statement> rewriteStatements(final List<Statement> statements) {
            final List<Statement> newStatements = new ArrayList<>();
            boolean changed = false;
            for (final Statement stmt : statements) {
                final List<Statement> replacement = tryRewriteNestedTernaryReturn(stmt);
                if (replacement == null) {
                    newStatements.add(stmt);
                } else {
                    newStatements.addAll(replacement);
                    changed = true;
                }
            }
            return changed ? newStatements : null;
        }

        private List<Statement> tryRewriteNestedTernaryReturn(final Statement stmt) {
            if (!isNestedTernaryReturn(stmt)) {
                return null;
            }
            final J.Ternary ternary = (J.Ternary) ((J.Return) stmt).getExpression();
            return parseReplacementStatements(ternary, stmt);
        }

        private List<Statement> parseReplacementStatements(final J.Ternary ternary, final Statement stmt) {
            final String ifElse = ternaryToIfElse(ternary, RESULT_VAR);
            final String wrapper = WRAPPER_TEMPLATE.formatted(ifElse);
            final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .logCompilationWarningsAndErrors(false)
                    .build().parse(wrapper).toList();
            if (parsed.isEmpty()) {
                return null;
            }
            final J.MethodDeclaration method = (J.MethodDeclaration)
                    ((J.CompilationUnit) parsed.getFirst())
                            .getClasses().getFirst().getBody().getStatements().getFirst();
            final List<Statement> replacement = new ArrayList<>();
            method.getBody().getStatements()
                    .forEach(s -> replacement.add(s.withPrefix(stmt.getPrefix())));
            return replacement;
        }
    }

    private static boolean isNestedTernaryReturn(final Statement stmt) {
        if (!(stmt instanceof J.Return ret) || ret.getExpression() == null) {
            return false;
        }
        if (!(ret.getExpression() instanceof J.Ternary ternary)) {
            return false;
        }
        return hasNestedTernary(ternary);
    }

    private static boolean hasNestedTernary(J.Ternary ternary) {
        return ternary.getTruePart() instanceof J.Ternary
                || ternary.getFalsePart() instanceof J.Ternary;
    }

    static String ternaryToIfElse(J.Ternary ternary, String varName) {
        final String condition = ternary.getCondition().toString().trim();
        final var truePart = ternary.getTruePart();
        final var falsePart = ternary.getFalsePart();

        final StringBuilder sb = new StringBuilder();
        sb.append("if (").append(condition).append(") { ")
                .append(varName).append(ASSIGNMENT).append(truePart.toString().trim()).append(BLOCK_END);

        if (falsePart instanceof J.Ternary nested) {
            sb.append(" else ").append(ternaryToIfElse(nested, varName));
        } else {
            sb.append(" else { ").append(varName).append(ASSIGNMENT)
                    .append(falsePart.toString().trim()).append(BLOCK_END);
        }

        return sb.toString();
    }
}
