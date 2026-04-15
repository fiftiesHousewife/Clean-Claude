package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultipleAssertRecipeTest {

    @Test
    void detectsThreeConsecutiveAsserts() {
        final var recipe = new MultipleAssertRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;
                class FooTest {
                    @Test
                    void checkStuff() {
                        String x = "hello";
                        assertThat(x).isNotNull();
                        assertThat(x).isEqualTo("hello");
                        assertThat(x).hasSize(5);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("checkStuff", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals(3, recipe.collectedRows().getFirst().assertCount())
        );
    }

    @Test
    void detectsTwoConsecutiveAsserts() {
        final var recipe = new MultipleAssertRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;
                class FooTest {
                    @Test
                    void checkStuff() {
                        String x = "hello";
                        assertThat(x).isNotNull();
                        assertThat(x).isEqualTo("hello");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresSingleAssert() {
        final var recipe = new MultipleAssertRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;
                class FooTest {
                    @Test
                    void checkStuff() {
                        String x = "hello";
                        assertThat(x).isNotNull();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonTestMethods() {
        final var recipe = new MultipleAssertRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import static org.assertj.core.api.Assertions.assertThat;
                class FooTest {
                    void helperMethod() {
                        String x = "hello";
                        assertThat(x).isNotNull();
                        assertThat(x).isEqualTo("hello");
                        assertThat(x).hasSize(5);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void countsLongestConsecutiveRun() {
        final var recipe = new MultipleAssertRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                class FooTest {
                    @Test
                    void checkStuff() {
                        String x = "hello";
                        assertEquals("hello", x);
                        assertTrue(x.length() > 0);
                        String y = x.toUpperCase();
                        assertFalse(y.isEmpty());
                        assertNotNull(y);
                        assertEquals("HELLO", y);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals(3, recipe.collectedRows().getFirst().assertCount())
        );
    }
}
