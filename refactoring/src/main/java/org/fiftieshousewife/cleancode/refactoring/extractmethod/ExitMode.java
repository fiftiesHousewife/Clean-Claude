package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the extracted range's control flow relates to its enclosing method.
 * IntelliJ's {@code ControlFlowWrapper} uses a real control-flow graph;
 * this port uses a mixed approach — AST analysis for {@code break}/
 * {@code continue} (we walk the extracted statements and check whether
 * each jump's enclosing loop or switch is itself inside the range) plus
 * a text-level scan for {@code return}/{@code throw}.
 *
 * <ul>
 *   <li>{@link #NONE} — no {@code return}/{@code throw} in the range and
 *       every {@code break}/{@code continue} targets a loop or switch
 *       that lives inside the range. Extracted method has the shape the
 *       analysis already computed.</li>
 *   <li>{@link #VOID_CONDITIONAL_EXIT} — every {@code return} inside the
 *       range is a bare {@code return;} and the enclosing method returns
 *       {@code void}. The extracted method returns {@code boolean};
 *       each in-range {@code return;} becomes {@code return true;};
 *       the call site wraps the invocation with {@code if (…) return;}.</li>
 * </ul>
 *
 * <p>Phase C (reference-type conditional exit) still requires sentinel
 * synthesis and is left for a follow-up; return inside a lambda or a
 * labeled break are still treated conservatively here.
 */
enum ExitMode {
    NONE,
    VOID_CONDITIONAL_EXIT;

    private static final Pattern RETURN = Pattern.compile("\\breturn\\b");
    private static final Pattern THROW = Pattern.compile("\\bthrow\\b");
    private static final Pattern BARE_RETURN = Pattern.compile("\\breturn\\s*([^\\s])");
    private static final String VOID = "void";

    static Optional<ExitMode> classify(final String rangeText,
                                       final List<Statement> rangeStatements,
                                       final J.MethodDeclaration enclosing,
                                       final Cursor cursor) {
        if (hasEscapingBreakOrContinue(rangeStatements)) {
            return Optional.empty();
        }
        if (THROW.matcher(rangeText).find()) {
            return Optional.empty();
        }
        if (!RETURN.matcher(rangeText).find()) {
            return Optional.of(NONE);
        }
        if (!allReturnsAreBare(rangeText)) {
            return Optional.empty();
        }
        if (!enclosingIsVoid(enclosing, cursor)) {
            return Optional.empty();
        }
        return Optional.of(VOID_CONDITIONAL_EXIT);
    }

    private static boolean hasEscapingBreakOrContinue(final List<Statement> rangeStatements) {
        final AtomicBoolean escapes = new AtomicBoolean(false);
        rangeStatements.forEach(statement -> new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Break visitBreak(final J.Break breakStmt, final AtomicBoolean flag) {
                if (!hasEnclosingBreakTarget(getCursor())) {
                    flag.set(true);
                }
                return breakStmt;
            }

            @Override
            public J.Continue visitContinue(final J.Continue cont, final AtomicBoolean flag) {
                if (!hasEnclosingLoop(getCursor())) {
                    flag.set(true);
                }
                return cont;
            }
        }.visit(statement, escapes));
        return escapes.get();
    }

    private static boolean hasEnclosingBreakTarget(final Cursor cursor) {
        return hasEnclosingLoop(cursor) || cursor.firstEnclosing(J.Switch.class) != null;
    }

    private static boolean hasEnclosingLoop(final Cursor cursor) {
        return cursor.firstEnclosing(J.ForLoop.class) != null
                || cursor.firstEnclosing(J.ForEachLoop.class) != null
                || cursor.firstEnclosing(J.WhileLoop.class) != null
                || cursor.firstEnclosing(J.DoWhileLoop.class) != null;
    }

    private static boolean allReturnsAreBare(final String rangeText) {
        final Matcher m = BARE_RETURN.matcher(rangeText);
        while (m.find()) {
            if (!";".equals(m.group(1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean enclosingIsVoid(final J.MethodDeclaration enclosing, final Cursor cursor) {
        return enclosing.getReturnTypeExpression() != null
                && VOID.equals(enclosing.getReturnTypeExpression().printTrimmed(cursor));
    }
}
