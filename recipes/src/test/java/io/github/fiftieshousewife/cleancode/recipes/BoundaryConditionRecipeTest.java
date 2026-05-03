package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryConditionRecipeTest {

    @Test
    void detectsRepeatedBoundaryExpressionInSameMethod() {
        // Per Clean Code G33 (Encapsulate Boundary Conditions), the
        // smell is the SAME boundary expression appearing twice — pull
        // it into a named variable to remove the duplication. The
        // book's example: `level + 1` used both in a comparison and
        // as a constructor argument.
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int parse(int[] tags, int level) {
                        if (tags.length > level) {
                            int next = level + 1;
                            return next;
                        }
                        return 0;
                    }
                    int twiceUsed(java.util.List<String> items) {
                        if (items.isEmpty()) return -1;
                        String last = items.get(items.size() - 1);
                        return items.size() - 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "items.size() - 1 appears twice in twiceUsed — extract to a named variable");
        assertEquals("twiceUsed", recipe.collectedRows().getFirst().methodName());
    }

    @Test
    void ignoresSingleIdiomaticBoundaryUse() {
        // A single `size() - 1` or `lastIndexOf('/') + 1` is the
        // idiomatic substring/last-element shape and adding a name
        // would only add ceremony.
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    String last(List<String> items) {
                        return items.get(items.size() - 1);
                    }
                    String basename(String path) {
                        return path.substring(path.lastIndexOf('/') + 1);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "single-use boundary expressions are idiomatic, not the G33 smell");
    }

    @Test
    void ignoresSimpleArithmeticWithoutMethodCall() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void loop() {
                        int i = 0;
                        int j = i + 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonBoundaryOperators() {
        final var recipe = new BoundaryConditionRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void bar(List<String> items) {
                        int doubled = items.size() * 2;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
