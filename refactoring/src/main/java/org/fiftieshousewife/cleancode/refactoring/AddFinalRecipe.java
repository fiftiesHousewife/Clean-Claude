package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddFinalRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add final to non-reassigned local variables";
    }

    @Override
    public String getDescription() {
        return "Adds the final modifier to local variables that are never reassigned after declaration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddFinalMethodVisitor();
    }

    private static final class AddFinalMethodVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if (m.getBody() == null) {
                return m;
            }
            final Set<String> reassigned = ReassignedVariableCollector.collect(m);
            return (J.MethodDeclaration) new AddFinalToLocalsVisitor().visit(m, reassigned);
        }
    }

    private static final class AddFinalToLocalsVisitor extends JavaIsoVisitor<Set<String>> {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                J.VariableDeclarations varDecls, Set<String> reassignedNames) {
            final J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, reassignedNames);
            if (shouldSkip(v, reassignedNames)) {
                return v;
            }
            return addFinalModifier(v);
        }
    }

    static boolean shouldSkip(J.VariableDeclarations v, Set<String> reassignedNames) {
        return isField(v) || isAlreadyFinal(v) || hasReassignedVariable(v, reassignedNames);
    }

    private static boolean hasReassignedVariable(J.VariableDeclarations v, Set<String> reassignedNames) {
        return v.getVariables().stream()
                .anyMatch(named -> reassignedNames.contains(named.getSimpleName()));
    }

    private static boolean isField(J.VariableDeclarations varDecls) {
        return varDecls.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static
                        || m.getType() == J.Modifier.Type.Private
                        || m.getType() == J.Modifier.Type.Protected
                        || m.getType() == J.Modifier.Type.Public);
    }

    private static boolean isAlreadyFinal(J.VariableDeclarations varDecls) {
        return varDecls.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
    }

    static J.VariableDeclarations addFinalModifier(J.VariableDeclarations varDecls) {
        final J.Modifier finalMod = new J.Modifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                J.Modifier.Type.Final,
                List.of());

        final List<J.Modifier> newModifiers = new ArrayList<>(varDecls.getModifiers());
        newModifiers.add(finalMod);

        final J.VariableDeclarations withMod = varDecls.withModifiers(newModifiers);

        if (varDecls.getTypeExpression() != null
                && varDecls.getTypeExpression().getPrefix().getWhitespace().isEmpty()) {
            return withMod.withTypeExpression(
                    varDecls.getTypeExpression().withPrefix(Space.SINGLE_SPACE));
        }
        return withMod;
    }
}
