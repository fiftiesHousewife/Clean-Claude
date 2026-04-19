package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.HashSet;
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

                final Set<String> reassigned = collectReassignedVariables(m);

                return (J.MethodDeclaration) new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(
                            J.VariableDeclarations varDecls, Set<String> reassignedNames) {
                        final J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, reassignedNames);

                        if (isField(v) || isAlreadyFinal(v)) {
                            return v;
                        }

                        final boolean anyReassigned = v.getVariables().stream()
                                .anyMatch(named -> reassignedNames.contains(named.getSimpleName()));
                        if (anyReassigned) {
                            return v;
                        }

                        return addFinalModifier(v);
                    }
                }.visit(m, reassigned);
            }
        };
    }

    private static Set<String> collectReassignedVariables(J.MethodDeclaration method) {
        final Set<String> reassigned = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
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
        }.visit(method.getBody(), reassigned);
        return reassigned;
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

    private static boolean isIncrementOrDecrement(J.Unary unary) {
        return unary.getOperator() == J.Unary.Type.PreIncrement
                || unary.getOperator() == J.Unary.Type.PreDecrement
                || unary.getOperator() == J.Unary.Type.PostIncrement
                || unary.getOperator() == J.Unary.Type.PostDecrement;
    }

    private static J.VariableDeclarations addFinalModifier(J.VariableDeclarations varDecls) {
        final var finalMod = new J.Modifier(
                org.openrewrite.Tree.randomId(),
                Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY,
                null,
                J.Modifier.Type.Final,
                java.util.List.of());

        final var newModifiers = new java.util.ArrayList<>(varDecls.getModifiers());
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
