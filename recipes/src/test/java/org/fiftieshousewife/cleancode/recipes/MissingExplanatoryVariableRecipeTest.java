package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingExplanatoryVariableRecipeTest {

    @Test
    void detectsChainedMethodCallInArgument() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getX().transform());
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresSingleMethodCallInArgument() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getName());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsComplexBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int compute(int a, int b, int c, int d) {
                        return a + b * c - d;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("compute", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresSimpleBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int add(int a, int b) {
                        return a + b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
