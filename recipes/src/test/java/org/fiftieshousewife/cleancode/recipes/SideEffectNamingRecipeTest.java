package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SideEffectNamingRecipeTest {

    @Test
    void detectsGetterWithAssignment() {
        final var recipe = new SideEffectNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int count;
                    public int getCount() {
                        count = count + 1;
                        return count;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("getCount", recipe.collectedRows().getFirst().methodName()),
                () -> assertTrue(recipe.collectedRows().getFirst().sideEffect().startsWith("assigns to"))
        );
    }

    @Test
    void detectsFinderWithFieldMutation() {
        final var recipe = new SideEffectNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Cache {
                    private int hits;
                    public String findValue(String key) {
                        hits = hits + 1;
                        return key;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("findValue", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresPureGetterWithNoAssignment() {
        final var recipe = new SideEffectNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String name;
                    public String getName() {
                        return name;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonQueryMethodWithAssignment() {
        final var recipe = new SideEffectNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int count;
                    public void incrementCount() {
                        count = count + 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
