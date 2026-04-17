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
import java.util.Optional;

public class RemoveNestedTernaryRecipe extends Recipe {

    private static final String RESULT_VAR = "result";
    private static final String ASSIGNMENT = " = ";
    private static final String BLOCK_CLOSE = "; }";
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
            final J.Block transformedBlock = super.visitBlock(block, ctx);
            final List<Statement> newStatements = new ArrayList<>();
            boolean changed = false;

            for (final Statement stmt : transformedBlock.getStatements()) {
                final List<Statement> replacement = rewriteStatement(stmt);
                if (replacement.isEmpty()) {
                    newStatements.add(stmt);
                } else {
                    newStatements.addAll(replacement);
                    changed = true;
                }
            }

            return changed ? transformedBlock.withStatements(newStatements) : transformedBlock;
        }

        private List<Statement> rewriteStatement(Statement stmt) {
            if (!isNestedTernaryReturn(stmt)) {
                return List.of();
            }
            final J.Ternary ternary = (J.Ternary) ((J.Return) stmt).getExpression();
            final String ifElse = ternaryToIfElse(ternary, RESULT_VAR);
            return parseReplacement(ifElse)
                    .map(statements -> applyPrefix(statements, stmt))
                    .orElseGet(List::of);
        }

        private Optional<List<Statement>> parseReplacement(String ifElseSource) {
            final String wrapper = WRAPPER_TEMPLATE.formatted(ifElseSource);
            final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .logCompilationWarningsAndErrors(false)
                    .build().parse(wrapper).toList();

            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            final J.MethodDeclaration method = (J.MethodDeclaration)
                    ((J.CompilationUnit) parsed.getFirst())
                            .getClasses().getFirst().getBody().getStatements().getFirst();
            return Optional.of(method.getBody().getStatements());
        }

        private List<Statement> applyPrefix(List<Statement> statements, Statement source) {
            final List<Statement> prefixed = new ArrayList<>();
            statements.forEach(s -> prefixed.add(s.withPrefix(source.getPrefix())));
            return prefixed;
        }
    }

    static boolean isNestedTernaryReturn(Statement stmt) {
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
        final String truePart = ternary.getTruePart().toString().trim();
        final var falsePart = ternary.getFalsePart();

        final StringBuilder sb = new StringBuilder();
        sb.append("if (").append(condition).append(") { ")
                .append(varName).append(ASSIGNMENT).append(truePart).append(BLOCK_CLOSE);

        if (falsePart instanceof J.Ternary nested) {
            sb.append(" else ").append(ternaryToIfElse(nested, varName));
        } else {
            sb.append(" else { ").append(varName).append(ASSIGNMENT)
                    .append(falsePart.toString().trim()).append(BLOCK_CLOSE);
        }

        return sb.toString();
    }
}
