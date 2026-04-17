package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtractExplanatoryVariableRecipe extends Recipe {

    private static final int NO_CHAIN = 0;
    private static final int LEAF_CHAIN = 1;
    private static final int CHAIN_INCREMENT = 1;

    private static final Map<String, String> NAME_BY_KEYWORD = Map.of(
            "startsWith", "hasExpectedPrefix",
            "equals", "isMatch",
            "contains", "containsTarget");
    private static final String FALLBACK_NAME = "condition";

    private final int minChainDepth;

    @JsonCreator
    public ExtractExplanatoryVariableRecipe(@JsonProperty("minChainDepth") final int minChainDepth) {
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
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block visited = super.visitBlock(block, ctx);
                final List<Statement> rewritten = new ArrayList<>();
                boolean changed = false;
                for (final Statement stmt : visited.getStatements()) {
                    final List<Statement> replacement = tryExtract(stmt);
                    if (replacement == null) {
                        rewritten.add(stmt);
                    } else {
                        rewritten.addAll(replacement);
                        changed = true;
                    }
                }
                return changed ? visited.withStatements(rewritten) : visited;
            }
        };
    }

    private List<Statement> tryExtract(final Statement stmt) {
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
        final J.Identifier varRef = identifier(varName);
        final J.If updated = ifStmt.withIfCondition(ifStmt.getIfCondition().withTree(varRef));
        return List.of(varDecl, updated);
    }

    private static Statement parseVariableDeclaration(final String varName, final String condText) {
        final String declSource = "class _T { void _m() { final var %s = %s; } }".formatted(varName, condText);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(declSource).toList();
        if (parsed.isEmpty()) {
            return null;
        }
        final J.CompilationUnit cu = (J.CompilationUnit) parsed.getFirst();
        final J.MethodDeclaration method = (J.MethodDeclaration) cu.getClasses().getFirst()
                .getBody().getStatements().getFirst();
        return method.getBody().getStatements().getFirst();
    }

    private static J.Identifier identifier(final String name) {
        return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), name, null, null);
    }

    static int chainDepth(final Expression expr) {
        if (expr instanceof J.MethodInvocation mi) {
            return chainDepthOfMethodInvocation(mi);
        }
        if (expr instanceof J.FieldAccess fa) {
            return CHAIN_INCREMENT + chainDepth(fa.getTarget());
        }
        if (expr instanceof J.Parentheses<?> p && p.getTree() instanceof Expression inner) {
            return chainDepth(inner);
        }
        if (expr instanceof J.Binary binary) {
            return Math.max(chainDepth(binary.getLeft()), chainDepth(binary.getRight()));
        }
        return NO_CHAIN;
    }

    private static int chainDepthOfMethodInvocation(final J.MethodInvocation mi) {
        return mi.getSelect() == null ? LEAF_CHAIN : CHAIN_INCREMENT + chainDepth(mi.getSelect());
    }

    private static String generateVariableName(final String conditionText) {
        return NAME_BY_KEYWORD.entrySet().stream()
                .filter(entry -> conditionText.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(FALLBACK_NAME);
    }
}
