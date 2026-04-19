package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UncheckedCastRecipeTest {

    @Test
    void detectsSuppressWarningsUncheckedOnMethod() {
        final var recipe = new UncheckedCastRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    @SuppressWarnings("unchecked")
                    <T> List<T> cast(Object obj) {
                        return (List<T>) obj;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("cast", recipe.collectedRows().getFirst().memberName())
        );
    }

    @Test
    void detectsSuppressWarningsUncheckedOnField() {
        final var recipe = new UncheckedCastRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                import java.util.HashMap;
                public class Foo {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = new HashMap();
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("data", recipe.collectedRows().getFirst().memberName())
        );
    }

    @Test
    void ignoresOtherSuppressWarningsValues() {
        final var recipe = new UncheckedCastRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("deprecation")
                    void oldMethod() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodsWithoutSuppressWarnings() {
        final var recipe = new UncheckedCastRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void clean() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
