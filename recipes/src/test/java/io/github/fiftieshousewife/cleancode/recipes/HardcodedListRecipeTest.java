package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HardcodedListRecipeTest {

    @Test
    void doesNotFlagStaticFinalCollectionInitializers() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Set;
                public class Foo {
                    private static final Set<String> PREFIXES = Set.of(
                        "validate", "check", "setup", "initializ", "creat", "build");
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "static final collection literals ARE the configuration, not a smell");
    }

    @Test
    void doesNotFlagStaticFinalMap() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    private static final Map<String, String> JUL_TO_LOG4J = Map.of(
                        "info", "INFO", "warning", "WARN", "severe", "ERROR");
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "static final lookup tables are collocated, type-checked configuration");
    }

    @Test
    void flagsLiteralsInsideMethodBody() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Set;
                public class Foo {
                    boolean isValidStatus(String s) {
                        var statuses = Set.of("ACTIVE", "PENDING", "DELETED", "ARCHIVED");
                        return statuses.contains(s);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("statuses", recipe.collectedRows().getFirst().fieldName()),
                () -> assertEquals(4, recipe.collectedRows().getFirst().literalCount()));
    }

    @Test
    void flagsInstanceFieldNotStaticFinal() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    private final List<String> defaults = List.of("a", "b", "c", "d");
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "non-static final-instance fields can still be hoisted to constants");
    }

    @Test
    void ignoresSmallCollections() {
        final var recipe = new HardcodedListRecipe(5);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Set;
                public class Foo {
                    boolean check(String s) {
                        return Set.of("a", "b").contains(s);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "below the threshold");
    }

    @Test
    void ignoresNonCollectionLiterals() {
        final var recipe = new HardcodedListRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void hi() {
                        String name = "hello";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "single-string locals are unrelated to G35");
    }
}
