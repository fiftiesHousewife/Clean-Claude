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
    void ignoresMethodWritingUnqualifiedInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class CsvParser {
                    private int rowsParsed;
                    public int parseRow(String row) {
                        int count = 0;
                        for (int i = 0; i < row.length(); i++) count++;
                        rowsParsed++;
                        return count;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "writing `rowsParsed++` without `this.` still counts as instance state");
    }

    @Test
    void ignoresMethodReadingUnqualifiedInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class SessionStore {
                    private final Map<String, String> sessions;
                    public SessionStore(Map<String, String> sessions) { this.sessions = sessions; }
                    public String lookupOrNull(String id) {
                        String normalised = id.trim();
                        String result = sessions.get(normalised);
                        return result;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "reading `sessions.get(...)` without `this.` still counts as instance state");
    }

    @Test
    void stillFlagsMethodUsingOnlyStaticFieldOfSameClass() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Calculator {
                    private static final int MULTIPLIER = 2;
                    public int doubled(int x) {
                        int step = x + 0;
                        int result = step * MULTIPLIER;
                        return result;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "reading a static field of the same class does NOT count as instance state");
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
