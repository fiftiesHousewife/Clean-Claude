package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LargeRecordRecipeTest {

    @Test
    void detectsRecordWithMoreThanFourComponents() {
        final var recipe = new LargeRecordRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public record Big(String a, String b, String c, int d, int e) {}
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals(5, recipe.collectedRows().getFirst().componentCount());
    }

    @Test
    void ignoresSmallRecord() {
        final var recipe = new LargeRecordRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public record Small(String a, String b) {}
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
