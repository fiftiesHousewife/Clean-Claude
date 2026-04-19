package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LargeConstructorRecipeTest {

    @Test
    void detectsConstructorWithSevenParameters() {
        final var recipe = new LargeConstructorRecipe(6);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Address {
                    Address(String line1, String line2, String city,
                            String state, String zip, String country, String phone) {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Address", recipe.collectedRows().getFirst().className()),
                () -> assertEquals(7, recipe.collectedRows().getFirst().parameterCount())
        );
    }

    @Test
    void ignoresConstructorWithSixParameters() {
        final var recipe = new LargeConstructorRecipe(6);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Point {
                    Point(int x, int y, int z, int w, int v, int u) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresRegularMethods() {
        final var recipe = new LargeConstructorRecipe(6);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void method(String a, String b, String c,
                                String d, String e, String f, String g) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void respectsCustomThreshold() {
        final var recipe = new LargeConstructorRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Foo(String a, String b, String c, String d) {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
