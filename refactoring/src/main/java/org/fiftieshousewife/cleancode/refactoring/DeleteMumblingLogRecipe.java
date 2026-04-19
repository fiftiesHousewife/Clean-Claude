package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Auto-deletes log statements whose entire payload is a compile-time
 * constant — the refactoring twin of
 * {@link org.fiftieshousewife.cleancode.recipes.FixedStringLogRecipe}.
 * Robert Martin classifies fixed-string log lines as clutter (G12);
 * this recipe takes the opinionated next step and removes them.
 *
 * <p>Same matchers as the detector: receiver is a conventional logger
 * field name (log/logger/LOG/LOGGER) or has a type whose simple name
 * is Logger; method name is one of the standard SLF4J levels; sole
 * argument is a string literal or a concatenation of literals only.
 *
 * <p>Two-arg parameterised calls and concatenations containing a
 * non-literal piece are left alone — they carry runtime information.
 */
public final class DeleteMumblingLogRecipe extends Recipe {

    private static final Set<String> LOG_LEVELS = Set.of(
            "trace", "debug", "info", "warn", "error");
    private static final Set<String> LOGGER_FIELD_NAMES = Set.of(
            "log", "logger", "LOG", "LOGGER");
    private static final String LOGGER_TYPE_SIMPLE_NAME = "Logger";

    @Override
    public String getDisplayName() {
        return "Delete fixed-string log calls";
    }

    @Override
    public String getDescription() {
        return "Removes logger calls whose entire payload is a compile-time "
                + "constant. Twin of FixedStringLogRecipe — detection flags, "
                + "this one deletes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block visited = super.visitBlock(block, ctx);
                final List<Statement> kept = new ArrayList<>(visited.getStatements().size());
                boolean changed = false;
                for (final Statement s : visited.getStatements()) {
                    if (s instanceof J.MethodInvocation mi
                            && isMumblingLogCall(mi)) {
                        changed = true;
                        continue;
                    }
                    kept.add(s);
                }
                return changed ? visited.withStatements(kept) : visited;
            }
        };
    }

    private static boolean isMumblingLogCall(final J.MethodInvocation mi) {
        return isLogLevelCall(mi) && isFixedStringPayload(mi);
    }

    private static boolean isLogLevelCall(final J.MethodInvocation mi) {
        if (!LOG_LEVELS.contains(mi.getSimpleName())) {
            return false;
        }
        final Expression select = mi.getSelect();
        if (!(select instanceof J.Identifier ident)) {
            return false;
        }
        if (LOGGER_FIELD_NAMES.contains(ident.getSimpleName())) {
            return true;
        }
        final JavaType type = ident.getType();
        if (type instanceof JavaType.FullyQualified fq) {
            return LOGGER_TYPE_SIMPLE_NAME.equals(fq.getClassName())
                    || fq.getClassName().endsWith("." + LOGGER_TYPE_SIMPLE_NAME);
        }
        return false;
    }

    private static boolean isFixedStringPayload(final J.MethodInvocation mi) {
        if (mi.getArguments().size() != 1) {
            return false;
        }
        return isConstantStringExpression(mi.getArguments().getFirst());
    }

    private static boolean isConstantStringExpression(final Expression expr) {
        if (expr instanceof J.Literal lit) {
            return lit.getType() == JavaType.Primitive.String && lit.getValue() instanceof String;
        }
        if (expr instanceof J.Binary bin && bin.getOperator() == J.Binary.Type.Addition) {
            return isConstantStringExpression(bin.getLeft())
                    && isConstantStringExpression(bin.getRight());
        }
        return false;
    }
}
