package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.NullDensityRecipe;
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
    void countsObjectsIsNullAndNonNullButNotRequireNonNull() {
        // Objects.requireNonNull is fail-fast boundary validation —
        // exactly the pattern Clean Code recommends — so counting it
        // as a null-density smell discourages the right habit.
        // Objects.isNull / nonNull are control-flow checks and still
        // count.
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Objects;
                public class Foo {
                    void dense(Object a, Object b, Object c) {
                        Objects.isNull(a);
                        Objects.nonNull(b);
                        Objects.isNull(c);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "isNull + nonNull + isNull = 3 control-flow null checks");
    }

    @Test
    void doesNotCountRequireNonNullBoundaryValidation() {
        // A method that fails fast on three null arguments via
        // requireNonNull is doing boundary validation right; it should
        // not be flagged as null-dense.
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Objects;
                public class Foo {
                    Foo(Object a, Object b, Object c) {
                        Objects.requireNonNull(a);
                        Objects.requireNonNull(b);
                        Objects.requireNonNull(c);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "fail-fast requireNonNull at the boundary is the recommended pattern, not a smell");
    }
}
