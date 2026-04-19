package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierEditorTest {

    @Test
    void createProducesModifierOfRequestedType() {
        assertEquals(J.Modifier.Type.Final, ModifierEditor.create(J.Modifier.Type.Final).getType());
    }

    @Test
    void appendAddsToEndAndPreservesExistingEntries() {
        final List<J.Modifier> base = List.of(
                ModifierEditor.create(J.Modifier.Type.Public),
                ModifierEditor.create(J.Modifier.Type.Static));
        final List<J.Modifier> result = ModifierEditor.append(base, J.Modifier.Type.Final);
        assertEquals(3, result.size());
        assertEquals(J.Modifier.Type.Final, result.get(2).getType());
    }

    @Test
    void removeFiltersMatchingType() {
        final List<J.Modifier> base = List.of(
                ModifierEditor.create(J.Modifier.Type.Private),
                ModifierEditor.create(J.Modifier.Type.Static));
        final List<J.Modifier> result = ModifierEditor.remove(base, J.Modifier.Type.Private);
        assertEquals(1, result.size());
        assertEquals(J.Modifier.Type.Static, result.get(0).getType());
    }

    @Test
    void hasReturnsTrueWhenTypePresent() {
        final List<J.Modifier> mods = List.of(ModifierEditor.create(J.Modifier.Type.Final));
        assertTrue(ModifierEditor.has(mods, J.Modifier.Type.Final));
        assertFalse(ModifierEditor.has(mods, J.Modifier.Type.Static));
    }

    @Test
    void hasAnyMatchesAcrossMultipleTypes() {
        final List<J.Modifier> mods = List.of(ModifierEditor.create(J.Modifier.Type.Protected));
        assertTrue(ModifierEditor.hasAny(mods, J.Modifier.Type.Public,
                J.Modifier.Type.Private, J.Modifier.Type.Protected));
        assertFalse(ModifierEditor.hasAny(mods, J.Modifier.Type.Public, J.Modifier.Type.Private));
    }
}
