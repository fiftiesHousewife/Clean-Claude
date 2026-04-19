package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strips section-marker comments from method bodies — the G34 pattern
 * where a method is annotated with {@code // Phase 1:}, {@code // Step 2:},
 * {@code // Section 3:} markers that give away mixed abstraction levels.
 * Only matches single-line comments whose trimmed content begins with
 * the marker keyword followed by an optional number; Javadoc and block
 * comments are left alone (they may carry genuine content).
 */
public final class DeleteSectionCommentsRecipe extends Recipe {

    private static final Pattern MARKER = Pattern.compile(
            "(?i)^(phase|step|section)(\\s+\\d+)?\\s*[:.\\-]?\\s*.*");

    @Override
    public String getDisplayName() {
        return "Delete section-marker comments";
    }

    @Override
    public String getDescription() {
        return "Removes single-line comments that read as section markers (e.g. `// Phase 1: "
                + "validate`) from method bodies. Fixes G34 by removing the cue that a method "
                + "mixes abstraction levels; the ExtractMethodRecipe is expected to have already "
                + "turned each section into its own helper.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block b = super.visitBlock(block, ctx);
                final List<Statement> statements = b.getStatements();
                final List<Statement> rewritten = new ArrayList<>(statements.size());
                boolean changed = false;
                for (final Statement statement : statements) {
                    final Space prefix = statement.getPrefix();
                    final List<Comment> kept = stripSectionComments(prefix.getComments());
                    if (kept.size() == prefix.getComments().size()) {
                        rewritten.add(statement);
                    } else {
                        rewritten.add(statement.withPrefix(prefix.withComments(kept)));
                        changed = true;
                    }
                }
                return changed ? b.withStatements(rewritten) : b;
            }
        };
    }

    private static List<Comment> stripSectionComments(final List<Comment> comments) {
        final List<Comment> kept = new ArrayList<>(comments.size());
        for (final Comment comment : comments) {
            if (isSectionMarker(comment)) {
                continue;
            }
            kept.add(comment);
        }
        return kept;
    }

    private static boolean isSectionMarker(final Comment comment) {
        if (!(comment instanceof TextComment text) || text.isMultiline()) {
            return false;
        }
        return MARKER.matcher(text.getText().trim()).matches();
    }
}
