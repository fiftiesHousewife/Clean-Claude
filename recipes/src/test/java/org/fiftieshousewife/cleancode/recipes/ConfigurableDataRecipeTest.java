package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurableDataRecipeTest {

    @Test
    void detectsMagicNumberInPrivateMethod() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int calculate() {
                        return 42;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("calculate", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("42", recipe.collectedRows().getFirst().literalValue())
        );
    }

    @Test
    void ignoresZeroAndOneLiterals() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int method() {
                        int a = 0;
                        int b = 1;
                        return a + b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresLiteralsInStaticFinalFields() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private static final int THRESHOLD = 42;
                    private int method() {
                        return 0;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresLiteralsInPublicMethods() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int calculate() {
                        return 42;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresLiteralsInTestClasses() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class FooTest {
                    private int helper() {
                        return 42;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsMultipleMagicNumbers() {
        final var recipe = new ConfigurableDataRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int calculate(int x) {
                        int a = x * 100;
                        int b = a + 50;
                        return b;
                    }
                }
                """);

        assertEquals(2, recipe.collectedRows().size());
    }
}
