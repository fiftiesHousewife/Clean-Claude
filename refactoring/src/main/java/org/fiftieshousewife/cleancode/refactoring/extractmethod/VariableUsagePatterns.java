package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import java.util.regex.Pattern;

/**
 * Text-level detection of whether a given variable name is read or
 * written inside a fragment of Java source. IntelliJ uses real
 * reference resolution for this; this port uses conservative regex
 * pattern matching. Over-inclusion is safe (a false positive turns
 * into a parameter we don't need, not incorrect generated code);
 * under-inclusion would silently generate broken code, so the patterns
 * err on the side of matching.
 */
final class VariableUsagePatterns {

    private static final String WRITE_SUFFIX = "=(?!=)|[+\\-*/%&|^]=|<<=|>>>?=|\\+\\+|--";
    private static final String INCREMENT_PREFIX = "(\\+\\+|--)";

    private VariableUsagePatterns() {}

    static boolean isRead(final String text, final String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find();
    }

    static boolean isWritten(final String text, final String name) {
        final String quoted = Pattern.quote(name);
        return Pattern.compile("\\b" + quoted + "\\s*(" + WRITE_SUFFIX + ")").matcher(text).find()
                || Pattern.compile(INCREMENT_PREFIX + "\\s*" + quoted + "\\b").matcher(text).find();
    }
}
