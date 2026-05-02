package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputArgumentRecipeTest {

    @Test
    void detectsMutatedCollectionParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void populate(List<String> results) {
                        results.add("item");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("results", recipe.collectedRows().getFirst().paramName());
    }

    @Test
    void ignoresReadOnlyParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    int countItems(List<String> items) {
                        return items.size();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateAccumulatorHelpers() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    private void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "Private accumulator helpers are an internal-plumbing pattern, "
                        + "not an API design issue — F2 is about contract surfaces");
    }

    @Test
    void ignoresStaticAccumulatorHelpers() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    static void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "Static helpers are typically internal builders/factories — "
                        + "the mutation contract is documented by the static modifier");
    }

    @Test
    void stillFlagsPublicMutatingApi() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "Public methods that mutate args are genuine API smells — F2 must still fire");
    }
}
