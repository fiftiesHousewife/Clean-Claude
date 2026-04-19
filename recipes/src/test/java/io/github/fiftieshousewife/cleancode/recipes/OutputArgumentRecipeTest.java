package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputArgumentRecipeTest {

    @Test
    void detectsMutatedCollectionParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void populate(List<String> results) {
                        results.add("item");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("results", recipe.collectedRows().getFirst().paramName());
    }

    @Test
    void ignoresReadOnlyParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    int countItems(List<String> items) {
                        return items.size();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
