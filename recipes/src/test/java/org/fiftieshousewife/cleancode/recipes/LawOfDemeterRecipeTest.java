package org.fiftieshousewife.cleancode.recipes;

import org.fiftieshousewife.cleancode.recipes.LawOfDemeterRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LawOfDemeterRecipeTest {

    @Test
    void detectsChainOfDepthThree() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object a;
                    void deep() {
                        a.getB().getC().getD();
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertTrue(recipe.collectedRows().getFirst().depth() >= 3);
    }

    @Test
    void ignoresChainOfDepthTwo() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object a;
                    void shallow() {
                        a.getB().getC();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void countsSingleCallAsDepthOne() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object a;
                    void single() {
                        a.getB();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
