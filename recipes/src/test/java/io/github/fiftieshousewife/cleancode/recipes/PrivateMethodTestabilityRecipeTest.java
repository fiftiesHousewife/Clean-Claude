package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivateMethodTestabilityRecipeTest {

    @Test
    void detectsLongPrivateMethod() {
        final var recipe = new PrivateMethodTestabilityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private void complexLogic() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                        int g = 7;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("complexLogic", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals(7, recipe.collectedRows().getFirst().lineCount())
        );
    }

    @Test
    void ignoresShortPrivateMethod() {
        final var recipe = new PrivateMethodTestabilityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int add(int a, int b) {
                        int sum = a + b;
                        return sum;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPackagePrivateAndPublicMethods() {
        final var recipe = new PrivateMethodTestabilityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void packageMethod() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                        int g = 7;
                    }
                    public void publicMethod() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                        int g = 7;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateConstructors() {
        final var recipe = new PrivateMethodTestabilityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private Foo() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                        int g = 7;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
