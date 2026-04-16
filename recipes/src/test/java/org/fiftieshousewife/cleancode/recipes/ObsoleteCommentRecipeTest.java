package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObsoleteCommentRecipeTest {

    @Test
    void detectsCommentReferencingNonExistentVariable() {
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // update the oldCounter variable
                        int newCounter = 0;
                        System.out.println(newCounter);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresCommentMatchingCode() {
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // increment the counter
                        int counter = 0;
                        counter++;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsCommentReferencingRenamedMethod() {
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    // calls fetchData to load the results
                    void process() {
                        loadResults();
                    }
                    void loadResults() {
                        System.out.println("loading");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
