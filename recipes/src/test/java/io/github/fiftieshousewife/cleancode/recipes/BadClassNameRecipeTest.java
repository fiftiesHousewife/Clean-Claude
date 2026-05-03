package io.github.fiftieshousewife.cleancode.recipes;

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

    @Test
    void ignoresHandlerSuffixWhenContractMandatesIt() {
        // com.sun.net.httpserver.HttpHandler dictates the *Handler
        // suffix for its implementations. Same pattern with AWS Lambda
        // handlers, jakarta servlet filters, etc. — the suffix isn't
        // the user's choice, so the 'rename to a noun' fix doesn't
        // apply.
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                interface HttpHandler {}
                public class ReportHandler implements HttpHandler {}
                """);

        assertEquals(0,
                recipe.collectedRows().stream()
                        .filter(r -> "ReportHandler".equals(r.className()))
                        .count(),
                "supertype suffix is contract-mandated — don't flag");
    }

    @Test
    void stillFlagsHandlerSuffixWhenSupertypeDoesNotShareTheSuffix() {
        final var recipe = new BadClassNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                interface Listener {}
                public class EventHandler implements Listener {}
                """);

        assertTrue(
                recipe.collectedRows().stream()
                        .anyMatch(r -> "EventHandler".equals(r.className())),
                "Listener doesn't end in Handler — the suffix is the user's choice, so flag it");
    }
}
