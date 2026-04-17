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
import java.util.HashSet;
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
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (m.getBody() == null) {
                    return m;
                }
                return applyFinalModifiers(m);
            }
        };
    }

    private static J.MethodDeclaration applyFinalModifiers(J.MethodDeclaration method) {
        final Set<String> reassigned = collectReassignedVariables(method);
        return (J.MethodDeclaration) new AddFinalToLocalsVisitor().visit(method, reassigned);
    }

    private static Set<String> collectReassignedVariables(J.MethodDeclaration method) {
        final Set<String> reassigned = new HashSet<>();
        new ReassignedNameCollector().visit(method.getBody(), reassigned);
        return reassigned;
    }

    static boolean isField(J.VariableDeclarations varDecls) {
        return varDecls.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static
                        || m.getType() == J.Modifier.Type.Private
                        || m.getType() == J.Modifier.Type.Protected
                        || m.getType() == J.Modifier.Type.Public);
    }

    static boolean isAlreadyFinal(J.VariableDeclarations varDecls) {
        return varDecls.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
    }

    static boolean isIncrementOrDecrement(J.Unary unary) {
        final J.Unary.Type op = unary.getOperator();
        return op == J.Unary.Type.PreIncrement
                || op == J.Unary.Type.PreDecrement
                || op == J.Unary.Type.PostIncrement
                || op == J.Unary.Type.PostDecrement;
    }

    static boolean isFinalCandidate(J.VariableDeclarations varDecls, Set<String> reassignedNames) {
        if (isField(varDecls) || isAlreadyFinal(varDecls)) {
            return false;
        }
        return varDecls.getVariables().stream()
                .noneMatch(named -> reassignedNames.contains(named.getSimpleName()));
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

    private static final class AddFinalToLocalsVisitor extends JavaIsoVisitor<Set<String>> {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                J.VariableDeclarations varDecls, Set<String> reassignedNames) {
            final J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, reassignedNames);
            if (isFinalCandidate(v, reassignedNames)) {
                return addFinalModifier(v);
            }
            return v;
        }
    }

    private static final class ReassignedNameCollector extends JavaIsoVisitor<Set<String>> {
        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, Set<String> names) {
            if (assignment.getVariable() instanceof J.Identifier ident) {
                names.add(ident.getSimpleName());
            }
            return super.visitAssignment(assignment, names);
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(
                J.AssignmentOperation assignOp, Set<String> names) {
            if (assignOp.getVariable() instanceof J.Identifier ident) {
                names.add(ident.getSimpleName());
            }
            return super.visitAssignmentOperation(assignOp, names);
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, Set<String> names) {
            if (isIncrementOrDecrement(unary) && unary.getExpression() instanceof J.Identifier ident) {
                names.add(ident.getSimpleName());
            }
            return super.visitUnary(unary, names);
        }
    }
}
