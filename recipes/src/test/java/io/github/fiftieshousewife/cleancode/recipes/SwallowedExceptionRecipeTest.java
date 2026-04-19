package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwallowedExceptionRecipeTest {

    @Test
    void detectsNamedIgnoredException() {
        final var recipe = new SwallowedExceptionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (Exception ignored) {
                        }
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
    void detectsEmptyCatchWithAnyName() {
        final var recipe = new SwallowedExceptionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (Exception e) {
                        }
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresCatchThatRethrows() {
        final var recipe = new SwallowedExceptionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresCatchThatLogs() {
        final var recipe = new SwallowedExceptionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
