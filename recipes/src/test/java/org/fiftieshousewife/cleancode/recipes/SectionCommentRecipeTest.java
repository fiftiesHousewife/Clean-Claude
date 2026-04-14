package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SectionCommentRecipeTest {

    @Test
    void detectsMethodWithMultipleSectionComments() {
        final var recipe = new SectionCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // Validate input
                        int x = 1;
                        // Transform data
                        int y = x + 1;
                        // Save results
                        int z = y + 1;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertTrue(recipe.collectedRows().getFirst().sectionCount() >= 2)
        );
    }

    @Test
    void ignoresMethodWithSingleComment() {
        final var recipe = new SectionCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void simple() {
                        // Validate input
                        int x = 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithNoComments() {
        final var recipe = new SectionCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void clean() {
                        int x = 1;
                        int y = 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
