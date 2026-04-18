package org.fiftieshousewife.cleancode.refactoring.movemethod;

import org.openrewrite.java.tree.J;

/**
 * Mutable accumulator for {@link MoveMethodRecipe}'s scanning pass.
 * The scanner fills {@link #captured} and {@link #sourceSimpleName}
 * when the target method is found; flips {@link #rejected} with a
 * {@link #rejectionReason} when any precondition fails. The applier
 * consults both and short-circuits to a no-op visitor when rejection
 * is set.
 */
final class MoveMethodState {

    J.MethodDeclaration captured;
    String sourceSimpleName = "";
    boolean rejected;
    String rejectionReason = "";

    void reject(final String reason) {
        rejected = true;
        rejectionReason = reason;
    }

    boolean isReady() {
        return !rejected && captured != null;
    }
}
