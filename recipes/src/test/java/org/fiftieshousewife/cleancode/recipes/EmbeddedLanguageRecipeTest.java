package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedLanguageRecipeTest {

    @Test
    void detectsEmbeddedHtmlInStringLiteral() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String render() {
                        return "<html><body><h1>Hello</h1></body></html>";
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("html", recipe.collectedRows().getFirst().language())
        );
    }

    @Test
    void detectsMultipleHtmlTags() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String render() {
                        return "<div class='container'><p>Hello World</p></div>";
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void detectsEmbeddedSql() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String query() {
                        return "SELECT * FROM users WHERE id = ?";
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("sql", recipe.collectedRows().getFirst().language())
        );
    }

    @Test
    void detectsEmbeddedCss() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String styles() {
                        return "body { font-family: sans-serif; color: #333; }";
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("css", recipe.collectedRows().getFirst().language())
        );
    }

    @Test
    void ignoresShortStrings() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String name() {
                        return "hello";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresFilePaths() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String path() {
                        return "/usr/local/bin/java";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void reportsMethodName() {
        final var recipe = new EmbeddedLanguageRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String buildPage() {
                        return "<html><body><h1>Title</h1></body></html>";
                    }
                }
                """);

        assertEquals("buildPage", recipe.collectedRows().getFirst().methodName());
    }
}
