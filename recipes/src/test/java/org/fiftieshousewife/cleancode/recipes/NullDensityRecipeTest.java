package org.fiftieshousewife.cleancode.recipes;

import org.fiftieshousewife.cleancode.recipes.NullDensityRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NullDensityRecipeTest {

    @Test
    void detectsMethodWithThreeOrMoreNullChecks() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void dense(Object a, Object b, Object c) {
                        if (a == null) {}
                        if (b != null) {}
                        if (c == null) {}
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals(3, recipe.collectedRows().getFirst().nullCheckCount());
    }

    @Test
    void ignoresMethodWithFewerThanThreeNullChecks() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void sparse(Object a) {
                        if (a == null) {}
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void countsObjectsIsNullCalls() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Objects;
                public class Foo {
                    void dense(Object a, Object b, Object c) {
                        Objects.isNull(a);
                        Objects.nonNull(b);
                        Objects.requireNonNull(c);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
