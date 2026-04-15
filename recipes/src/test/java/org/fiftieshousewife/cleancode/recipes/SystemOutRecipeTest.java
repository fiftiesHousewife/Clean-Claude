package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemOutRecipeTest {

    @Test
    void detectsSystemOutPrintln() {
        final var recipe = new SystemOutRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        System.out.println("debug");
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("System.out.println", recipe.collectedRows().getFirst().call())
        );
    }

    @Test
    void detectsSystemErrPrintln() {
        final var recipe = new SystemOutRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        System.err.println("error");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsPrintStackTrace() {
        final var recipe = new SystemOutRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        try {
                            int x = 1;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("printStackTrace", recipe.collectedRows().getFirst().call())
        );
    }

    @Test
    void ignoresLoggerCalls() {
        final var recipe = new SystemOutRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method() {
                        String message = "hello";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
