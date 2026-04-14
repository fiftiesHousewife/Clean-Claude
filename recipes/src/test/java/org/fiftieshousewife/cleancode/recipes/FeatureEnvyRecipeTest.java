package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureEnvyRecipeTest {

    @Test
    void detectsMethodEnvyingAnotherObject() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        bar.doA();
                        bar.doB();
                        bar.doC();
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("bar", recipe.collectedRows().getFirst().enviedClass()),
                () -> assertEquals(0, recipe.collectedRows().getFirst().selfCallCount()),
                () -> assertEquals(3, recipe.collectedRows().getFirst().externalCallCount())
        );
    }

    @Test
    void ignoresMethodWithMoreSelfCalls() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void balanced() {
                        doX();
                        doY();
                        doZ();
                        bar.doA();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithFewerThanThreeCalls() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void tiny() {
                        bar.doA();
                        bar.doB();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresStaticMethodsAndConstructors() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    static Object bar;
                    static void process() {
                        bar.doA();
                        bar.doB();
                        bar.doC();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
