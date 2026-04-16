package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

public class MoveDeclarationRecipe extends Recipe {

    private final int minDistance;

    @JsonCreator
    public MoveDeclarationRecipe(@JsonProperty("minDistance") int minDistance) {
        this.minDistance = minDistance;
    }

    @Override
    public String getDisplayName() {
        return "Move variable declarations closer to first use";
    }

    @Override
    public String getDescription() {
        return "Moves local variable declarations that are %d+ statements away from their first use. Fixes G10."
                .formatted(minDistance);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                final J.Block b = super.visitBlock(block, ctx);
                final List<Statement> statements = b.getStatements();
                if (statements.size() < minDistance + 1) {
                    return b;
                }

                final List<Statement> reordered = new ArrayList<>(statements);
                boolean changed = false;

                for (int i = 0; i < reordered.size(); i++) {
                    if (!(reordered.get(i) instanceof J.VariableDeclarations varDecl)) {
                        continue;
                    }
                    if (isStaticOrFinal(varDecl)) {
                        continue;
                    }

                    final String varName = varDecl.getVariables().getFirst().getSimpleName();
                    final int firstUse = findFirstUse(reordered, varName, i + 1);

                    if (firstUse < 0 || (firstUse - i) < minDistance) {
                        continue;
                    }

                    final Statement removed = reordered.remove(i);
                    reordered.add(firstUse - 1, removed);
                    changed = true;
                    i--;
                }

                return changed ? b.withStatements(reordered) : b;
            }
        };
    }

    private static boolean isStaticOrFinal(J.VariableDeclarations varDecl) {
        return varDecl.getModifiers().stream().anyMatch(m ->
                m.getType() == J.Modifier.Type.Static || m.getType() == J.Modifier.Type.Final);
    }

    private static int findFirstUse(List<Statement> statements, String varName, int startIndex) {
        for (int i = startIndex; i < statements.size(); i++) {
            if (statements.get(i).toString().contains(varName)) {
                return i;
            }
        }
        return -1;
    }
}
