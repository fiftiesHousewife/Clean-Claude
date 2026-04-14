package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortVariableNameRecipeTest {

    @Test
    void detectsSingleLetterLocalVariable() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process() {
                        String s = "hello";
                        int n = 42;
                    }
                }
                """);

        assertEquals(2, recipe.collectedRows().size());
    }

    @Test
    void detectsSingleLetterMethodParameter() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process(String s, int n) {}
                }
                """);

        assertEquals(2, recipe.collectedRows().size());
    }

    @Test
    void allowsSingleLetterInForLoop() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void count() {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(i);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void allowsSingleLetterInForEachLoop() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void each(List<String> items) {
                        for (String s : items) {
                            System.out.println(s);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void allowsSingleLetterInLambda() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void each(List<String> items) {
                        items.forEach(s -> System.out.println(s));
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void allowsKnownShortNames() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void handle(Exception e) {
                        String id = "abc";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void allowsTwoCharacterNames() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process() {
                        String sb = "builder";
                        int ct = 0;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void includesMethodAndClassNameInRow() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class MyService {
                    public void transform() {
                        String s = "value";
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("MyService", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("transform", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("s", recipe.collectedRows().getFirst().variableName())
        );
    }
}
