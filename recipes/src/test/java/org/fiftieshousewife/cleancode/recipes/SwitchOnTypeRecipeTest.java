package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwitchOnTypeRecipeTest {

    @Test
    void detectsDeepInstanceofChain() {
        final var recipe = new SwitchOnTypeRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void dispatch(Object o) {
                        if (o instanceof String) {}
                        else if (o instanceof Integer) {}
                        else if (o instanceof Double) {}
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresShallowChain() {
        final var recipe = new SwitchOnTypeRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void dispatch(Object o) {
                        if (o instanceof String) {}
                        else if (o instanceof Integer) {}
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
