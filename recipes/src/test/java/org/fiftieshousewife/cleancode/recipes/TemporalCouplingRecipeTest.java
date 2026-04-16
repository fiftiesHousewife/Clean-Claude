package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemporalCouplingRecipeTest {

    @Test
    void detectsSequenceOfVoidCallsWithNoDependency() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void setup() {
                        init();
                        configure();
                        start();
                    }
                    void init() {}
                    void configure() {}
                    void start() {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("setup", recipe.collectedRows().getFirst().methodName()),
                () -> assertTrue(recipe.collectedRows().getFirst().callCount() >= 3)
        );
    }

    @Test
    void ignoresMethodsWithReturnValueChaining() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        String a = load();
                        String b = transform(a);
                        save(b);
                    }
                    String load() { return ""; }
                    String transform(String s) { return s; }
                    void save(String s) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresFewConsecutiveCalls() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void setup() {
                        init();
                        start();
                    }
                    void init() {}
                    void start() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
