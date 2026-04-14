package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MumblingCommentRecipeTest {

    @Test
    void detectsCommentRestatingMethodName() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void fetchAllPets() {
                        // Fetches all pets
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsCommentRestatingParameters() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void create(String name, int age) {
                        // Pass the name and age
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresMeaningfulComment() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void configure() {
                        // HttpGraphQlClient.url() replaces the base URL rather than appending
                        int x = 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void normalisesComparison() {
        assertEquals("fetch all pets", MumblingCommentRecipe.normalise("Fetch all pets"));
        assertEquals("fetch all pets", MumblingCommentRecipe.normalise("fetch-all-pets"));
    }

    @Test
    void convertsCamelToWords() {
        assertEquals("fetch All Pets", MumblingCommentRecipe.camelToWords("fetchAllPets"));
        assertEquals("get User By Id", MumblingCommentRecipe.camelToWords("getUserById"));
    }

    @Test
    void ignoresDoubleSlashInsideStringLiteral() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void buildUrl() {
                        String url = "https://example.com/api";
                        int x = 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresFormattedStringContainingDoubleSlash() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void report(String name, int line) {
                        String msg = "Mumbling comment in '%s': %s //extra".formatted(name, line);
                        int x = 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresVeryShortComments() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void doSomething() {
                        int x = 1; // })
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void stillDetectsRealMumblingCommentOnLineWithString() {
        final var recipe = new MumblingCommentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void fetchAllPets() {
                        String s = "hello"; // fetch all pets
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
