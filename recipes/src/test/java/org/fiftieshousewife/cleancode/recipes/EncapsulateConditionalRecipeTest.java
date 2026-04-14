package org.fiftieshousewife.cleancode.recipes;

import org.fiftieshousewife.cleancode.recipes.EncapsulateConditionalRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncapsulateConditionalRecipeTest {

    @Test
    void detectsComplexConditionWithMultipleLogicalOperators() {
        final var recipe = new EncapsulateConditionalRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void check(boolean a, boolean b, boolean c) {
                        if (a && b && c) {}
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertTrue(recipe.collectedRows().getFirst().depth() >= 2);
    }

    @Test
    void ignoresSimpleCondition() {
        final var recipe = new EncapsulateConditionalRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void check(boolean a) {
                        if (a) {}
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresSingleLogicalOperator() {
        final var recipe = new EncapsulateConditionalRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void check(boolean a, boolean b) {
                        if (a && b) {}
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
