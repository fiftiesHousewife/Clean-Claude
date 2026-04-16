package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuppressedWarningRecipeTest {

    @Test
    void detectsSuppressWarningsUnchecked() {
        final var recipe = new SuppressedWarningRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("unchecked")
                    void process() {
                        Object obj = new Object();
                        java.util.List<String> list = (java.util.List<String>) obj;
                        System.out.println(list);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("unchecked", recipe.collectedRows().getFirst().warningType())
        );
    }

    @Test
    void detectsSuppressWarningsRawTypes() {
        final var recipe = new SuppressedWarningRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("rawtypes")
                    void process() {
                        java.util.List list = new java.util.ArrayList();
                        System.out.println(list);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresThisEscapeSuppression() {
        final var recipe = new SuppressedWarningRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("this-escape")
                    public Foo() {
                        System.out.println("init");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
