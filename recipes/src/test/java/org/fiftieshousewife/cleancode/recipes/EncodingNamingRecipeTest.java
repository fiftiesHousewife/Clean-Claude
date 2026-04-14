package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncodingNamingRecipeTest {

    @Test
    void detectsHungarianNotation() {
        final var recipe = new EncodingNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        String strName = "test";
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("Hungarian notation", recipe.collectedRows().getFirst().violationType());
    }

    @Test
    void detectsIPrefixOnInterface() {
        final var recipe = new EncodingNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public interface IRepository {}
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("I-prefix", recipe.collectedRows().getFirst().violationType());
    }

    @Test
    void ignoresNormalNames() {
        final var recipe = new EncodingNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        String name = "test";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
