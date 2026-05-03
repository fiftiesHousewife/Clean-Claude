package io.github.fiftieshousewife.cleancode.recipes;

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

    @Test
    void capturesPascalCaseIdentifiersWithoutDroppingLeadingLetter() {
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // delegates to HarnessRecipePass for the heavy lifting
                        int counter = 0;
                        System.out.println(counter);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("HarnessRecipePass", recipe.collectedRows().get(0).missingIdentifier(),
                "regex must keep the leading uppercase letter");
    }

    @Test
    void doesNotFlagAcronymsThatLookLikeIdentifiersButAreNot() {
        // MCP, HTML, URL — all-uppercase acronyms slip through the
        // CAMEL_CASE_PATTERN because it only requires "at least one
        // uppercase letter somewhere". A real Java type/member name
        // always has at least one lowercase letter, so a mixed-case
        // post-filter rejects acronyms without further infrastructure.
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // forwards stdin/stdout via MCP
                        // emits HTML for browsers
                        // resolves the URL once at startup
                        int counter = 0;
                        System.out.println(counter);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "acronyms (MCP, HTML, URL) are not Java identifiers and must not be flagged");
    }

    @Test
    void doesNotFlagShortTokensRegardlessOfCase() {
        // Two-letter shapes like 'AB' or 'Id' are too short to
        // confidently treat as a class/member reference; reject them.
        final var recipe = new ObsoleteCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        // see Id and AB above
                        int counter = 0;
                        System.out.println(counter);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "tokens shorter than 3 chars are too noisy to flag");
    }
}
