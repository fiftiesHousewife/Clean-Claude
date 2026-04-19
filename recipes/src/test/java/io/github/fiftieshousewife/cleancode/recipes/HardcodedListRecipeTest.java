package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HardcodedListRecipeTest {

    @Test
    void detectsSetOfWithManyStringLiterals() {
        final var recipe = new HardcodedListRecipe(5);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Set;
                public class Foo {
                    private static final Set<String> PREFIXES = Set.of(
                        "validate", "check", "setup", "initializ", "creat", "build",
                        "transform", "convert", "process");
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("PREFIXES", recipe.collectedRows().getFirst().fieldName()),
                () -> assertTrue(recipe.collectedRows().getFirst().literalCount() >= 5)
        );
    }

    @Test
    void detectsListOfWithManyLiterals() {
        final var recipe = new HardcodedListRecipe(5);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    private static final List<String> NAMES = List.of(
                        "alice", "bob", "charlie", "diana", "eve");
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresSmallSets() {
        final var recipe = new HardcodedListRecipe(5);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Set;
                public class Foo {
                    private static final Set<String> SMALL = Set.of("a", "b");
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonCollectionFields() {
        final var recipe = new HardcodedListRecipe(5);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private static final String NAME = "hello";
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsMapOfWithManyEntries() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    private static final Map<String, Integer> SIZES = Map.of(
                        "small", 1, "medium", 2, "large", 3);
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
