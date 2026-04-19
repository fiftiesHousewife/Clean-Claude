package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NestedTernaryRecipeTest {

    @Test
    void detectsTernaryNestedInTrueBranch() {
        final var recipe = new NestedTernaryRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int pick(boolean a, boolean b) {
                        return a ? (b ? 1 : 2) : 3;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("pick", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals(2, recipe.collectedRows().getFirst().depth())
        );
    }

    @Test
    void detectsTernaryNestedInFalseBranch() {
        final var recipe = new NestedTernaryRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int pick(boolean a, boolean b) {
                        return a ? 1 : (b ? 2 : 3);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals(2, recipe.collectedRows().getFirst().depth())
        );
    }

    @Test
    void ignoresSimpleTernary() {
        final var recipe = new NestedTernaryRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int pick(boolean a) {
                        return a ? 1 : 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsTriplyNestedTernaryAsDepthThree() {
        final var recipe = new NestedTernaryRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int pick(boolean a, boolean b, boolean c) {
                        return a ? (b ? (c ? 1 : 2) : 3) : 4;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals(3, recipe.collectedRows().getFirst().depth())
        );
    }
}
