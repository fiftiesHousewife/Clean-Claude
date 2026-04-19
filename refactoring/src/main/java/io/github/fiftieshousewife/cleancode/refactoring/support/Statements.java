package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helpers for rebuilding {@link J.Block} statement lists inside
 * refactoring recipes. Replaces the common
 * {@code new ArrayList + boolean changed + for-loop + withStatements}
 * scaffolding that surfaced as CPD duplication across several recipes.
 */
public final class Statements {

    private Statements() {}

    /**
     * Walks the block's statements, applies {@code expander} to each,
     * and returns a block with the concatenation of every result. When
     * the expander returns {@code List.of(stmt)} (i.e. the exact input
     * statement, unchanged) the entry passes through untouched. When
     * it returns zero or multiple statements, or a different single
     * statement, the block is rebuilt.
     *
     * <p>Returns the original block unchanged when every statement
     * passed through. Lets recipes express "for each stmt, decide
     * what replaces it" without hand-rolling the change-tracking
     * bookkeeping.
     */
    public static J.Block rebuild(final J.Block block,
                                  final Function<Statement, List<Statement>> expander) {
        final List<Statement> oldStatements = block.getStatements();
        final List<Statement> newStatements = new ArrayList<>(oldStatements.size());
        boolean changed = false;
        for (final Statement stmt : oldStatements) {
            final List<Statement> expanded = expander.apply(stmt);
            if (expanded.size() == 1 && expanded.get(0) == stmt) {
                newStatements.add(stmt);
            } else {
                newStatements.addAll(expanded);
                changed = true;
            }
        }
        return changed ? block.withStatements(newStatements) : block;
    }
}
