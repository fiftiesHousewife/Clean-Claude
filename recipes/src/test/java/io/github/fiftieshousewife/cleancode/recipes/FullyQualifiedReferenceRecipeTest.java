package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FullyQualifiedReferenceRecipeTest {

    @Test
    void flagsInlineFullyQualifiedParameterType() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void register(java.util.List<String> items) {}
                }
                """);

        final var rows = recipe.collectedRows();
        assertAll(
                () -> assertEquals(1, rows.size()),
                () -> assertTrue(rows.getFirst().samplePreview().contains("java.util.List"),
                        "preview should include the offending reference, got: "
                                + rows.getFirst().samplePreview()));
    }

    @Test
    void doesNotFlagImportedTypes() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void register(List<String> items) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "an imported type used by short name is not a FQN reference");
    }

    @Test
    void doesNotFlagImportStatementsThemselves() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.Map;
                public class Foo {
                    List<String> items;
                    Map<String, String> lookup;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "the FQN inside an import statement is required and should not be flagged");
    }

    @Test
    void collapsesMultipleReferencesInSameFileToOneFinding() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void a(java.util.List<String> xs) {}
                    void b(java.util.Map<String, String> ys) {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size(),
                        "per-file aggregation: one row per source file regardless of reference count"),
                () -> assertEquals(2, recipe.collectedRows().getFirst().count(),
                        "count reflects the number of FQN references in the file"));
    }
}
