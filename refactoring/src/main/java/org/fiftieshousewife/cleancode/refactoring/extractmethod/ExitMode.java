package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the extracted range's control flow relates to its enclosing method.
 * IntelliJ's {@code ControlFlowWrapper} uses real control-flow analysis;
 * this port uses a text-based classifier that is conservative by
 * construction — anything it cannot cleanly prove is treated as a reject.
 *
 * <ul>
 *   <li>{@link #NONE} — no {@code return}/{@code break}/{@code continue}/
 *       {@code throw} reaches outside the range. Extracted method has the
 *       shape the analysis already computed.</li>
 *   <li>{@link #VOID_CONDITIONAL_EXIT} — every {@code return} inside the
 *       range is a bare {@code return;} and the enclosing method returns
 *       {@code void}. The extracted method returns {@code boolean};
 *       each in-range {@code return;} becomes {@code return true;};
 *       the call site wraps the invocation with {@code if (…) return;}.</li>
 * </ul>
 *
 * <p>Phase-C (reference-type conditional exit) and break/continue handling
 * require a real CFG to distinguish escaping exits from ones that target a
 * loop inside the range. Those are left for a follow-up.
 */
enum ExitMode {
    NONE,
    VOID_CONDITIONAL_EXIT;

    private static final Pattern RETURN = Pattern.compile("\\breturn\\b");
    private static final Pattern BREAK = Pattern.compile("\\bbreak\\b");
    private static final Pattern CONTINUE = Pattern.compile("\\bcontinue\\b");
    private static final Pattern THROW = Pattern.compile("\\bthrow\\b");
    private static final Pattern BARE_RETURN = Pattern.compile("\\breturn\\s*([^\\s])");
    private static final String VOID = "void";

    static Optional<ExitMode> classify(final String rangeText,
                                       final J.MethodDeclaration enclosing,
                                       final Cursor cursor) {
        final boolean hasBreak = BREAK.matcher(rangeText).find();
        final boolean hasContinue = CONTINUE.matcher(rangeText).find();
        final boolean hasThrow = THROW.matcher(rangeText).find();
        if (hasBreak || hasContinue || hasThrow) {
            return Optional.empty();
        }
        final boolean hasReturn = RETURN.matcher(rangeText).find();
        if (!hasReturn) {
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
