package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BroadCatchRecipeTest {

    @Test
    void detectsCatchException() {
        final var recipe = new BroadCatchRecipe();
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

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("Exception", recipe.collectedRows().getFirst().caughtType())
        );
    }

    @Test
    void detectsCatchThrowable() {
        final var recipe = new BroadCatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresSpecificCatch() {
        final var recipe = new BroadCatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.IOException;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresRuntimeException() {
        final var recipe = new BroadCatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        try {
                            int x = 1;
                        } catch (RuntimeException e) {
                            throw e;
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
