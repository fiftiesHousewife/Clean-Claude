package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumForConstantsRecipeTest {

    @Test
    void detectsConstantsWithSharedPrefix() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    static final int STATUS_ACTIVE = 1;
                    static final int STATUS_INACTIVE = 2;
                    static final int STATUS_PENDING = 3;
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("STATUS", recipe.collectedRows().getFirst().prefix());
    }

    @Test
    void ignoresFewConstants() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    static final int STATUS_ACTIVE = 1;
                    static final int STATUS_INACTIVE = 2;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
