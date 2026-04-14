package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicStringRecipeTest {

    @Test
    void detectsDuplicatedStringLiteral() {
        final var recipe = new MagicStringRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void one() {
                        String a = "hello world";
                        String b = "hello world";
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        final var row = recipe.collectedRows().getFirst();
        assertAll(
                () -> assertEquals("Foo", row.className()),
                () -> assertEquals("hello world", row.value()),
                () -> assertEquals(2, row.count())
        );
    }

    @Test
    void ignoresStringsThatAppearOnlyOnce() {
        final var recipe = new MagicStringRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void one() {
                        String a = "unique string";
                        String b = "another string";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresEmptyStringsAndShortStrings() {
        final var recipe = new MagicStringRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void one() {
                        String a = "";
                        String b = "";
                        String c = "ab";
                        String d = "ab";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresAnnotationStringValues() {
        final var recipe = new MagicStringRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("unchecked")
                    void one() {}
                    @SuppressWarnings("unchecked")
                    void two() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
