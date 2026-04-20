package io.github.fiftieshousewife.cleancode.refactoring;

import io.github.fiftieshousewife.cleancode.refactoring.support.ModifierEditor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds {@code final} to local variable declarations and method
 * parameters that the method body never reassigns.
 *
 * <p>Deliberately skipped:
 * <ul>
 *   <li><b>Lambda parameters.</b> Syntactically legal, but the CLAUDE.md
 *       convention keeps lambda params unannotated and the old recipe
 *       corrupted the declaration when there was no whitespace to
 *       adopt as the inserted modifier's prefix.</li>
 *   <li><b>Catch-clause parameters.</b> The {@code final} there does
 *       nothing — the caught exception can't be reassigned anyway.</li>
 *   <li><b>Try-with-resources variables.</b> Already implicitly final
 *       by the language spec.</li>
 *   <li><b>For-each loop variables.</b> Same reasoning as catch.</li>
 *   <li><b>Fields.</b> Outside the recipe's scope; different rules.</li>
 * </ul>
 *
 * <p>When the variable already has at least one modifier
 * ({@code public}, {@code @Nullable}-resolved, etc.), the new
 * {@code final} is appended with a leading {@code Space.SINGLE_SPACE}
 * so it separates cleanly. When there are no prior modifiers,
 * {@code final} inherits the type expression's original leading
 * prefix and the type expression is reset to a single space, so the
 * resulting print is always well-separated between annotation/type.
 */
public class AddFinalRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add final to non-reassigned local variables";
    }

    @Override
    public String getDescription() {
        return "Adds the final modifier to local variables and method parameters that are "
                + "never reassigned after declaration. Skips lambda, catch, try-with-resources, "
                + "and for-each parameters — where final is either already implied or would "
                + "produce awkward whitespace.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    final J.MethodDeclaration method, final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (m.getBody() == null) {
                    return m;
                }

                final Set<String> reassigned = collectReassignedVariables(m);

                return (J.MethodDeclaration) new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(
                            final J.VariableDeclarations varDecls, final Set<String> reassignedNames) {
                        final J.VariableDeclarations v = super.visitVariableDeclarations(varDecls, reassignedNames);

                        if (!isEligiblePosition(getCursor())) {
                            return v;
                        }
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

    /**
     * Returns true when this VariableDeclarations is in a position where
     * applying {@code final} is useful and syntactically well-formed.
     * Skips lambda params, catch parameters, try-with-resources resources,
     * and for-each loop variables.
     */
    private static boolean isEligiblePosition(final Cursor cursor) {
        final Object parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.Lambda.Parameters) {
            return false;
        }
        if (parent instanceof J.Lambda) {
            return false;
        }
        if (parent instanceof J.Try.Resource) {
            return false;
        }
        if (parent instanceof J.ForEachLoop.Control) {
            return false;
        }
        if (parent instanceof J.Try.Catch) {
            return false;
        }
        if (parent instanceof J.ControlParentheses) {
            return false;
        }
        return true;
    }

    private static Set<String> collectReassignedVariables(final J.MethodDeclaration method) {
        final Set<String> reassigned = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Assignment visitAssignment(final J.Assignment assignment, final Set<String> names) {
                if (assignment.getVariable() instanceof J.Identifier ident) {
                    names.add(ident.getSimpleName());
                }
                return super.visitAssignment(assignment, names);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(
                    final J.AssignmentOperation assignOp, final Set<String> names) {
                if (assignOp.getVariable() instanceof J.Identifier ident) {
                    names.add(ident.getSimpleName());
                }
                return super.visitAssignmentOperation(assignOp, names);
            }

            @Override
            public J.Unary visitUnary(final J.Unary unary, final Set<String> names) {
                if (isIncrementOrDecrement(unary) && unary.getExpression() instanceof J.Identifier ident) {
                    names.add(ident.getSimpleName());
                }
                return super.visitUnary(unary, names);
            }
        }.visit(method.getBody(), reassigned);
        return reassigned;
    }

    private static boolean isField(final J.VariableDeclarations varDecls) {
        return ModifierEditor.hasAny(varDecls.getModifiers(),
                J.Modifier.Type.Static, J.Modifier.Type.Private,
                J.Modifier.Type.Protected, J.Modifier.Type.Public);
    }

    private static boolean isAlreadyFinal(final J.VariableDeclarations varDecls) {
        return ModifierEditor.has(varDecls.getModifiers(), J.Modifier.Type.Final);
    }

    private static boolean isIncrementOrDecrement(final J.Unary unary) {
        return unary.getOperator() == J.Unary.Type.PreIncrement
                || unary.getOperator() == J.Unary.Type.PreDecrement
                || unary.getOperator() == J.Unary.Type.PostIncrement
                || unary.getOperator() == J.Unary.Type.PostDecrement;
    }

    private static J.VariableDeclarations addFinalModifier(final J.VariableDeclarations varDecls) {
        final boolean hasPriorModifiers = !varDecls.getModifiers().isEmpty();
        if (hasPriorModifiers) {
            return varDecls.withModifiers(
                    ModifierEditor.append(varDecls.getModifiers(), J.Modifier.Type.Final));
        }
        if (varDecls.getTypeExpression() == null) {
            return varDecls;
        }
        // No prior modifiers: the type expression's prefix is the whitespace
        // between whatever precedes it (annotations, opening paren, etc.) and
        // the type name. Transfer that prefix to the new `final` and reset
        // the type's prefix to a single space so `final` and the type name
        // are correctly separated.
        final Space typeOriginalPrefix = varDecls.getTypeExpression().getPrefix();
        final J.Modifier finalMod = new J.Modifier(
                Tree.randomId(),
                typeOriginalPrefix,
                Markers.EMPTY,
                null,
                J.Modifier.Type.Final,
                List.of());
        return varDecls
                .withModifiers(List.of(finalMod))
                .withTypeExpression(varDecls.getTypeExpression().withPrefix(Space.SINGLE_SPACE));
    }
}
