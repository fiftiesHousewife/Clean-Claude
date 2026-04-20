package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Folds consecutive expression statements that all invoke a fluent
 * builder method on the same receiver into a single chained expression:
 *
 * <pre>
 *   sb.append("a");
 *   sb.append("b");
 *   sb.append("c");
 * </pre>
 *
 * becomes
 *
 * <pre>
 *   sb.append("a").append("b").append("c");
 * </pre>
 *
 * <p>Only folds when the receiver is the same simple identifier across
 * the run and the receiver's declared type is one of the known-fluent
 * types (StringBuilder, StringBuffer). Other types are left alone to
 * avoid folding calls that return {@code void} or a different type.
 *
 * <p>Fixes the G31 "consecutive void calls with no data dependency"
 * reading of builder blocks and the G12-ish readability benefit of
 * seeing the build sequence as one expression.
 */
public final class ChainConsecutiveBuilderCallsRecipe extends Recipe {

    private static final Set<String> FLUENT_RECEIVER_TYPES = Set.of(
            "java.lang.StringBuilder", "java.lang.StringBuffer");

    // Methods on StringBuilder/StringBuffer that return the receiver and
    // therefore compose into a chain. Every other method (setLength,
    // trimToSize, ensureCapacity, setCharAt, getChars, deleteCharAt-or-
    // delete overloads that return void on older JDKs, etc.) returns
    // void or a non-receiver type and cannot be followed by .append(…).
    private static final Set<String> FLUENT_METHOD_NAMES = Set.of(
            "append", "appendCodePoint", "insert", "delete", "deleteCharAt",
            "replace", "reverse");

    @Override
    public String getDisplayName() {
        return "Chain consecutive fluent-builder calls on the same receiver";
    }

    @Override
    public String getDescription() {
        return "Folds runs of `receiver.method(...);` expression statements on the same identifier "
                + "into one chained call when the receiver is a StringBuilder or StringBuffer. "
                + "Leaves non-fluent types (List, Map) alone since their mutator methods don't "
                + "return the receiver.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block b = super.visitBlock(block, ctx);
                final List<Statement> source = b.getStatements();
                final List<Statement> rewritten = new ArrayList<>(source.size());
                boolean changed = false;
                int i = 0;
                while (i < source.size()) {
                    final int runEnd = findRunEnd(source, i);
                    if (runEnd > i) {
                        rewritten.add(chainRun(source, i, runEnd));
                        changed = true;
                        i = runEnd + 1;
                    } else {
                        rewritten.add(source.get(i));
                        i++;
                    }
                }
                return changed ? b.withStatements(rewritten) : b;
            }
        };
    }

    private static int findRunEnd(final List<Statement> statements, final int start) {
        final String receiverName = receiverIdentifierName(statements.get(start));
        if (receiverName == null) {
            return start;
        }
        int end = start;
        for (int j = start + 1; j < statements.size(); j++) {
            if (!receiverName.equals(receiverIdentifierName(statements.get(j)))) {
                break;
            }
            end = j;
        }
        return end;
    }

    private static Statement chainRun(final List<Statement> statements, final int start, final int end) {
        J.MethodInvocation chained = methodInvocationOf(statements.get(start));
        for (int j = start + 1; j <= end; j++) {
            final J.MethodInvocation next = methodInvocationOf(statements.get(j));
            chained = spliceIntoLeftmostReceiver(next, chained.withPrefix(Space.EMPTY));
        }
        return chained.withPrefix(statements.get(start).getPrefix());
    }

    /**
     * Replaces the leftmost receiver of {@code target} with {@code newReceiver}.
     * Walks down the select chain so that a {@code next} statement of the form
     * {@code sb.append(a).append(b)} has its bare {@code sb} receiver swapped
     * out for the previously-chained expression — without losing any of the
     * intermediate {@code .append(...)} calls that live between the leftmost
     * receiver and the outer invocation.
     */
    private static J.MethodInvocation spliceIntoLeftmostReceiver(
            final J.MethodInvocation target, final Expression newReceiver) {
        if (target.getSelect() instanceof J.MethodInvocation inner) {
            return target.withSelect(spliceIntoLeftmostReceiver(inner, newReceiver));
        }
        return target.withSelect(newReceiver);
    }

    private static String receiverIdentifierName(final Statement statement) {
        final J.MethodInvocation mi = methodInvocationOf(statement);
        if (mi == null || !everyCallInChainIsFluent(mi)) {
            return null;
        }
        final Expression receiver = leftmostReceiver(mi);
        if (!(receiver instanceof J.Identifier id)) {
            return null;
        }
        if (!hasFluentType(id.getType())) {
            return null;
        }
        return id.getSimpleName();
    }

    /**
     * Walks the select chain to confirm every hop is a fluent receiver-
     * returning method ({@code append}, {@code insert}, …). A single
     * non-fluent hop — e.g. {@code sb.setLength(0)} which returns void —
     * disqualifies the whole statement from being merged into a chain:
     * you can't follow {@code .setLength(...)} with {@code .append(…)}.
     */
    private static boolean everyCallInChainIsFluent(final J.MethodInvocation mi) {
        if (!FLUENT_METHOD_NAMES.contains(mi.getSimpleName())) {
            return false;
        }
        if (mi.getSelect() instanceof J.MethodInvocation inner) {
            return everyCallInChainIsFluent(inner);
        }
        return true;
    }

    private static Expression leftmostReceiver(final J.MethodInvocation mi) {
        Expression select = mi.getSelect();
        while (select instanceof J.MethodInvocation inner) {
            select = inner.getSelect();
        }
        return select;
    }

    private static J.MethodInvocation methodInvocationOf(final Statement statement) {
        return statement instanceof J.MethodInvocation mi ? mi : null;
    }

    private static boolean hasFluentType(final JavaType type) {
        if (type == null) {
            return false;
        }
        return FLUENT_RECEIVER_TYPES.contains(type.toString());
    }
}
