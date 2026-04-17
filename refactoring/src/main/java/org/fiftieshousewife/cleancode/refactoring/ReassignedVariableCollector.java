package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.Set;

final class ReassignedVariableCollector extends JavaIsoVisitor<Set<String>> {

    private final Set<String> reassigned = new HashSet<>();

    static Set<String> collect(J.MethodDeclaration method) {
        final ReassignedVariableCollector collector = new ReassignedVariableCollector();
        collector.visit(method.getBody(), collector.reassigned);
        return collector.reassigned;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, Set<String> context) {
        if (assignment.getVariable() instanceof J.Identifier ident) {
            reassigned.add(ident.getSimpleName());
        }
        return super.visitAssignment(assignment, context);
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Set<String> context) {
        if (assignOp.getVariable() instanceof J.Identifier ident) {
            reassigned.add(ident.getSimpleName());
        }
        return super.visitAssignmentOperation(assignOp, context);
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, Set<String> context) {
        if (isIncrementOrDecrement(unary) && unary.getExpression() instanceof J.Identifier ident) {
            reassigned.add(ident.getSimpleName());
        }
        return super.visitUnary(unary, context);
    }

    private static boolean isIncrementOrDecrement(J.Unary unary) {
        final J.Unary.Type operator = unary.getOperator();
        final boolean isPreIncrementOrDecrement =
                operator == J.Unary.Type.PreIncrement || operator == J.Unary.Type.PreDecrement;
        final boolean isPostIncrementOrDecrement =
                operator == J.Unary.Type.PostIncrement || operator == J.Unary.Type.PostDecrement;
        return isPreIncrementOrDecrement || isPostIncrementOrDecrement;
    }
}
