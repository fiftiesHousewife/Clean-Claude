package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SectionCommentRecipeTest {

    @Test
    void detectsMethodWithMultipleSectionComments() {
        final var recipe = new SectionCommentRecipe(1);
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
    void detectsAnyInlineCommentNotJustVerbs() {
        final var recipe = new SectionCommentRecipe(1);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void doWork() {
                        // notify the listeners
                        System.out.println("notifying");
                        // dispatch the events
                        System.out.println("dispatching");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsSingleCommentWithDefaultThreshold() {
        final var recipe = new SectionCommentRecipe(1);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void simple() {
                        // Validate input
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void respectsHigherThreshold() {
        final var recipe = new SectionCommentRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // step one
                        int x = 1;
                        // step two
                        int y = 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithNoComments() {
        final var recipe = new SectionCommentRecipe(1);
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

    @Test
    void ignoresTodoAndFixmeComments() {
        final var recipe = new SectionCommentRecipe(1);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // TODO: fix this later
                        int x = 1;
                        // FIXME: handle null
                        int y = 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresSuppressionComments() {
        final var recipe = new SectionCommentRecipe(1);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // noinspection unchecked
                        int x = 1;
                        // NOPMD
                        int y = 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
