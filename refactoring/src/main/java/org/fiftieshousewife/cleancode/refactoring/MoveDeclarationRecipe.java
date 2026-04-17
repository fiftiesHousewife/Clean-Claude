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
        return new MoveDeclarationVisitor(minDistance);
    }

    private static final class MoveDeclarationVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final int minDistance;

        private MoveDeclarationVisitor(final int minDistance) {
            this.minDistance = minDistance;
        }

        @Override
        public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
            final J.Block visitedBlock = super.visitBlock(block, ctx);
            final List<Statement> statements = visitedBlock.getStatements();
            if (statements.size() < minDistance + 1) {
                return visitedBlock;
            }
            return reorderDeclarations(visitedBlock, statements);
        }

        private J.Block reorderDeclarations(final J.Block block, final List<Statement> statements) {
            final List<Statement> reordered = new ArrayList<>(statements);
            final boolean changed = moveDeclarationsInPlace(reordered);
            return changed ? block.withStatements(reordered) : block;
        }

        private boolean moveDeclarationsInPlace(final List<Statement> reordered) {
            boolean changed = false;
            for (int i = 0; i < reordered.size(); i++) {
                final int newIndex = tryMoveDeclarationAt(reordered, i);
                if (newIndex != i) {
                    changed = true;
                    i = newIndex;
                }
            }
            return changed;
        }

        private int tryMoveDeclarationAt(final List<Statement> reordered, final int index) {
            if (!(reordered.get(index) instanceof J.VariableDeclarations varDecl)) {
                return index;
            }
            if (isStaticOrFinal(varDecl)) {
                return index;
            }
            final String varName = varDecl.getVariables().getFirst().getSimpleName();
            final int firstUse = findFirstUse(reordered, varName, index + 1);
            if (firstUse < 0 || (firstUse - index) < minDistance) {
                return index;
            }
            final Statement removed = reordered.remove(index);
            reordered.add(firstUse - 1, removed);
            return index - 1;
        }
    }

    private static boolean isStaticOrFinal(final J.VariableDeclarations varDecl) {
        return varDecl.getModifiers().stream().anyMatch(m ->
                m.getType() == J.Modifier.Type.Static || m.getType() == J.Modifier.Type.Final);
    }

    private static int findFirstUse(final List<Statement> statements, final String varName, final int startIndex) {
        for (int i = startIndex; i < statements.size(); i++) {
            if (statements.get(i).toString().contains(varName)) {
                return i;
            }
        }
        return -1;
    }
}
