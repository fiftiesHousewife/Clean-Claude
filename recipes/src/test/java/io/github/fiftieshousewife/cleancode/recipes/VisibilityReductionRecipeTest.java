package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityReductionRecipeTest {

    @Test
    void detectsPublicMutableField() {
        final var recipe = new VisibilityReductionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String name;
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("name", recipe.collectedRows().getFirst().fieldName()),
                () -> assertEquals("public", recipe.collectedRows().getFirst().currentVisibility())
        );
    }

    @Test
    void ignoresPublicFinalField() {
        final var recipe = new VisibilityReductionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public final String name = "hello";
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPublicStaticField() {
        final var recipe = new VisibilityReductionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public static String name;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateField() {
        final var recipe = new VisibilityReductionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String name;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
