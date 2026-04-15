package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InappropriateStaticRecipeTest {

    @Test
    void detectsMethodNotUsingInstanceState() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("add", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresMethodWithOverrideAnnotation() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @Override
                    public String toString() {
                        String result = "Foo";
                        String padded = result + "!";
                        return padded;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodAccessingInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String name;
                    public String greet() {
                        String greeting = "Hello";
                        String full = greeting + " " + this.name;
                        return full;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodCallingInstanceMethod() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String greet() {
                        String name = getName();
                        String greeting = "Hello " + name;
                        return greeting;
                    }
                    private String getName() { return "World"; }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresStaticMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public static int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresShortMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresClassesImplementingInterfaces() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo implements Comparable<Foo> {
                    public int compareTo(Foo other) {
                        int a = 1;
                        int b = 2;
                        return a - b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
