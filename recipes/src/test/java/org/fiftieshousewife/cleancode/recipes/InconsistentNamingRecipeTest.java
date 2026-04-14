package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InconsistentNamingRecipeTest {

    @Test
    void detectsGetAndFetchMix() {
        final var recipe = new InconsistentNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Service {
                    public String getUser() { return null; }
                    public String fetchOrder() { return null; }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Service", recipe.collectedRows().getFirst().className()),
                () -> assertTrue(recipe.collectedRows().getFirst().conflictingPrefixes().contains("get")),
                () -> assertTrue(recipe.collectedRows().getFirst().conflictingPrefixes().contains("fetch"))
        );
    }

    @Test
    void detectsCreateAndMakeMix() {
        final var recipe = new InconsistentNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Factory {
                    public Object createWidget() { return null; }
                    public Object makeGadget() { return null; }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertTrue(recipe.collectedRows().getFirst().conflictingPrefixes().contains("create")),
                () -> assertTrue(recipe.collectedRows().getFirst().conflictingPrefixes().contains("make"))
        );
    }

    @Test
    void ignoresConsistentNaming() {
        final var recipe = new InconsistentNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Service {
                    public String getUser() { return null; }
                    public String getOrder() { return null; }
                    public String getName() { return null; }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodsWithoutConflictingPrefixes() {
        final var recipe = new InconsistentNamingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Util {
                    public void process() {}
                    public void handle() {}
                    public void compute() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
