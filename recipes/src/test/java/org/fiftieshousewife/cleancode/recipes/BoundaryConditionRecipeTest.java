package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryConditionRecipeTest {

    @Test
    void detectsSizeMinusOne() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void bar(List<String> items) {
                        int last = items.size() - 1;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("bar", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void detectsMethodCallPlusOne() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int next(String text) {
                        return text.indexOf('x') + 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresSimpleArithmeticWithoutMethodCall() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void loop() {
                        int i = 0;
                        int j = i + 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonBoundaryOperators() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void bar(List<String> items) {
                        int doubled = items.size() * 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
