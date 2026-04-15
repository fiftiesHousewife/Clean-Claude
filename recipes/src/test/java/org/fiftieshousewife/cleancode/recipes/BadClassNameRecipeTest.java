package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BadClassNameRecipeTest {

    @Test
    void detectsHelperSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class PaginationHelper {}
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("PaginationHelper", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("Helper", recipe.collectedRows().getFirst().suffix())
        );
    }

    @Test
    void detectsUtilSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class CsvUtil {}
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsUtilsSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class StringUtils {}
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsManagerSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class SessionManager {}
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsProcessorSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class DataProcessor {}
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresCleanNames() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class CsvResponse {}
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresHandlerSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class EventHandler {}
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
