package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.openrewrite.java.tree.J;

/**
 * Predicates that gate recipes by class-declaration kind.
 *
 * <p>Most modifier-editing recipes assume they're working on a regular
 * {@code class} — where conventional OOP visibility, static-vs-instance,
 * and constant-field semantics apply. Interfaces, records, and
 * annotation declarations all have different rules:
 *
 * <ul>
 *   <li>Interface methods can't be {@code static} without changing
 *       dispatch; fields are implicitly {@code public static final} so
 *       an explicit {@code private static final} prepended field is
 *       illegal; interface {@code private} methods (Java 9+) are a
 *       real feature that removing {@code private} from would break.</li>
 *   <li>Record bodies contain compact accessors that reference
 *       components via implicit {@code this}; making them static
 *       produces a non-static-context error.</li>
 *   <li>Annotation bodies are attribute declarations, not callable
 *       methods.</li>
 * </ul>
 *
 * <p>Enums are treated as regular classes here — enum methods behave
 * the same as ordinary instance methods for the purposes of every
 * recipe in this codebase.
 */
public final class ClassKinds {

    private ClassKinds() {}

    /**
     * Returns true when the declaration is a regular {@code class} or
     * {@code enum}. False for interfaces, records, and annotations.
     */
    public static boolean isRegularClass(final J.ClassDeclaration cd) {
        final J.ClassDeclaration.Kind.Type kind = cd.getKind();
        return kind == J.ClassDeclaration.Kind.Type.Class
                || kind == J.ClassDeclaration.Kind.Type.Enum;
    }

    /** True when the declaration is an {@code interface}. */
    public static boolean isInterface(final J.ClassDeclaration cd) {
        return cd.getKind() == J.ClassDeclaration.Kind.Type.Interface;
    }

    /** True when the declaration is a {@code record}. */
    public static boolean isRecord(final J.ClassDeclaration cd) {
        return cd.getKind() == J.ClassDeclaration.Kind.Type.Record;
    }
}
