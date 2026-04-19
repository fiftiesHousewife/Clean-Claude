package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for mutating {@link J.Modifier} lists on declarations.
 * Refactoring recipes routinely need to add a modifier (e.g. Final) or
 * strip one (e.g. Private). Each recipe still owns its
 * {@code withModifiers} call — the declaration types differ — but the
 * list construction and presence checks now live here.
 */
public final class ModifierEditor {

    private ModifierEditor() {}

    public static J.Modifier create(final J.Modifier.Type type) {
        return new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                null, type, List.of());
    }

    public static List<J.Modifier> append(final List<J.Modifier> modifiers,
                                          final J.Modifier.Type type) {
        final List<J.Modifier> result = new ArrayList<>(modifiers);
        result.add(create(type));
        return result;
    }

    public static List<J.Modifier> remove(final List<J.Modifier> modifiers,
                                          final J.Modifier.Type type) {
        return modifiers.stream().filter(m -> m.getType() != type).toList();
    }

    public static boolean has(final List<J.Modifier> modifiers, final J.Modifier.Type type) {
        return modifiers.stream().anyMatch(m -> m.getType() == type);
    }

    public static boolean hasAny(final List<J.Modifier> modifiers, final J.Modifier.Type... types) {
        for (final J.Modifier.Type type : types) {
            if (has(modifiers, type)) {
                return true;
            }
        }
        return false;
    }
}
