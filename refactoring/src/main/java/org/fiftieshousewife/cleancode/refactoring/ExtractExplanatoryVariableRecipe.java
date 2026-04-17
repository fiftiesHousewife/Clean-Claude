package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

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
                return rewriteBlock(b);
            }
        };
    }

    J.Block rewriteBlock(final J.Block block) {
        final List<Statement> newStatements = new ArrayList<>();
        boolean changed = false;

        for (final Statement stmt : block.getStatements()) {
            final List<Statement> replacement = tryExtractCondition(stmt);
            if (replacement == null) {
                newStatements.add(stmt);
            } else {
                newStatements.addAll(replacement);
                changed = true;
            }
        }

        return changed ? block.withStatements(newStatements) : block;
    }

    List<Statement> tryExtractCondition(final Statement stmt) {
        if (!(stmt instanceof J.If ifStmt)) {
            return null;
        }

        final Expression condition = ifStmt.getIfCondition().getTree();
        if (chainDepth(condition) < minChainDepth) {
            return null;
        }

        final String condText = condition.toString().trim();
        final String varName = generateVariableName(condText);
        final Statement varDecl = parseVariableDeclaration(varName, condText);
        if (varDecl == null) {
            return null;
        }

        final J.If rewrittenIf = ifStmt.withIfCondition(
                ifStmt.getIfCondition().withTree(newIdentifier(varName)));
        return List.of(varDecl, rewrittenIf);
    }

    private static Statement parseVariableDeclaration(final String varName, final String condText) {
        final String declSource = "class _T { void _m() { final var %s = %s; } }"
                .formatted(varName, condText);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(declSource).toList();
        if (parsed.isEmpty()) {
            return null;
        }
        final J.MethodDeclaration method = (J.MethodDeclaration) ((J.CompilationUnit) parsed.getFirst())
                .getClasses().getFirst().getBody().getStatements().getFirst();
        return method.getBody().getStatements().getFirst();
    }

    private static J.Identifier newIdentifier(final String varName) {
        return new J.Identifier(
                org.openrewrite.Tree.randomId(),
                org.openrewrite.java.tree.Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY,
                List.of(), varName, null, null);
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
