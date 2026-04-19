package io.github.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AST-level detection of whether a given variable name is read or
 * written inside a list of statements. Replaces the earlier regex-based
 * VariableUsagePatterns, which over-included by matching identifiers
 * inside comments and string literals and could not distinguish
 * {@code foo.bar} from a bare read of {@code bar}.
 *
 * <p>Walks the OpenRewrite {@link J} tree. Identifier references that
 * appear in non-read positions — method names, field-access selectors,
 * declaration names, and the LHS of plain assignments — are filtered
 * out. Comments and string literals never surface as identifiers in
 * the AST, so they can never match by accident.
 *
 * <p>Compound assignments ({@code name += 1}) count as BOTH a read and
 * a write, matching Java semantics: the RHS expression reads the
 * current value before storing the new one.
 */
final class VariableUsageAnalyzer {

    private VariableUsageAnalyzer() {}

    static boolean isRead(final List<? extends J> nodes, final String name) {
        final AtomicBoolean found = new AtomicBoolean(false);
        final JavaIsoVisitor<AtomicBoolean> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier id, final AtomicBoolean hit) {
                if (hit.get() || !name.equals(id.getSimpleName())) {
                    return id;
                }
                if (isReadPosition()) {
                    hit.set(true);
                }
                return id;
            }

            private boolean isReadPosition() {
                final Object parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.VariableDeclarations.NamedVariable nv
                        && nv.getName() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.FieldAccess fa && fa.getName() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.MethodInvocation mi && mi.getName() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.NewClass nc && nc.getClazz() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.Assignment a && a.getVariable() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.Label label && label.getLabel() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.Break brk && brk.getLabel() == getCursor().getValue()) {
                    return false;
                }
                if (parent instanceof J.Continue cnt && cnt.getLabel() == getCursor().getValue()) {
                    return false;
                }
                return true;
            }
        };
        nodes.forEach(node -> visitor.visit(node, found));
        return found.get();
    }

    static boolean isWritten(final List<? extends J> nodes, final String name) {
        final AtomicBoolean found = new AtomicBoolean(false);
        final JavaIsoVisitor<AtomicBoolean> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.Assignment visitAssignment(final J.Assignment assignment, final AtomicBoolean hit) {
                if (!hit.get() && targetsName(assignment.getVariable())) {
                    hit.set(true);
                }
                return super.visitAssignment(assignment, hit);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(
                    final J.AssignmentOperation op, final AtomicBoolean hit) {
                if (!hit.get() && targetsName(op.getVariable())) {
                    hit.set(true);
                }
                return super.visitAssignmentOperation(op, hit);
            }

            @Override
            public J.Unary visitUnary(final J.Unary unary, final AtomicBoolean hit) {
                if (!hit.get() && isIncOrDec(unary.getOperator()) && targetsName(unary.getExpression())) {
                    hit.set(true);
                }
                return super.visitUnary(unary, hit);
            }

            private boolean targetsName(final J target) {
                return target instanceof J.Identifier id && name.equals(id.getSimpleName());
            }

            private boolean isIncOrDec(final J.Unary.Type op) {
                return op == J.Unary.Type.PreIncrement || op == J.Unary.Type.PostIncrement
                        || op == J.Unary.Type.PreDecrement || op == J.Unary.Type.PostDecrement;
            }
        };
        nodes.forEach(node -> visitor.visit(node, found));
        return found.get();
    }
}
