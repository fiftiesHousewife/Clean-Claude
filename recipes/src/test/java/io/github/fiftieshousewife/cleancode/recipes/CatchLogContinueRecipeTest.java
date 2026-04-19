package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.CatchLogContinueRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatchLogContinueRecipeTest {

    @Test
    void detectsCatchWithOnlyLogging() {
        final var recipe = new CatchLogContinueRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Foo {
                    private static final Logger log = LoggerFactory.getLogger(Foo.class);
                    void doStuff() {
                        try {
                            riskyCall();
                        } catch (Exception e) {
                            log.error("Failed", e);
                        }
                    }
                    void riskyCall() throws Exception {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("doStuff", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void detectsEmptyCatchBlock() {
        final var recipe = new CatchLogContinueRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void doStuff() {
                        try { riskyCall(); } catch (Exception e) {}
                    }
                    void riskyCall() throws Exception {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresCatchThatRethrows() {
        final var recipe = new CatchLogContinueRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void doStuff() {
                        try {
                            riskyCall();
                        } catch (Exception e) {
                            throw new RuntimeException("wrapped", e);
                        }
                    }
                    void riskyCall() throws Exception {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresCatchWithLogAndRethrow() {
        final var recipe = new CatchLogContinueRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Foo {
                    private static final Logger log = LoggerFactory.getLogger(Foo.class);
                    void doStuff() {
                        try {
                            riskyCall();
                        } catch (Exception e) {
                            log.error("Failed", e);
                            throw new RuntimeException(e);
                        }
                    }
                    void riskyCall() throws Exception {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsCatchWithOnlyPrintStackTrace() {
        final var recipe = new CatchLogContinueRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void doStuff() {
                        try { riskyCall(); } catch (Exception e) { e.printStackTrace(); }
                    }
                    void riskyCall() throws Exception {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
