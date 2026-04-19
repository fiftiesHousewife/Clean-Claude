package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.DisabledTestRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisabledTestRecipeTest {

    @Test
    void detectsDisabledWithoutReason() {
        final var recipe = new DisabledTestRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Disabled;
                import org.junit.jupiter.api.Test;
                public class FooTest {
                    @Disabled
                    @Test void skippedTest() {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertFalse(recipe.collectedRows().getFirst().hasReason());
    }

    @Test
    void detectsDisabledWithTodoReason() {
        final var recipe = new DisabledTestRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Disabled;
                import org.junit.jupiter.api.Test;
                public class FooTest {
                    @Disabled("TODO")
                    @Test void skippedTest() {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresDisabledWithMeaningfulReason() {
        final var recipe = new DisabledTestRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.junit.jupiter.api.Disabled;
                import org.junit.jupiter.api.Test;
                public class FooTest {
                    @Disabled("Blocked by JIRA-1234")
                    @Test void skippedTest() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
