package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumForConstantsRecipeTest {

    @Test
    void detectsConstantsWithSharedPrefix() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    static final int STATUS_ACTIVE = 1;
                    static final int STATUS_INACTIVE = 2;
                    static final int STATUS_PENDING = 3;
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("STATUS", recipe.collectedRows().getFirst().prefix());
    }

    @Test
    void ignoresPrivateSingleUseConstantsInSingleExpression() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Visitor {
                    private static final String SLF4J_TRACE = "trace";
                    private static final String SLF4J_DEBUG = "debug";
                    private static final String SLF4J_INFO = "info";
                    private static final String SLF4J_WARN = "warn";
                    private static final String SLF4J_ERROR = "error";
                    boolean isLogLevel(final String name) {
                        return SLF4J_TRACE.equals(name) || SLF4J_DEBUG.equals(name)
                                || SLF4J_INFO.equals(name) || SLF4J_WARN.equals(name)
                                || SLF4J_ERROR.equals(name);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "J3.1: five private constants each referenced exactly once in a single "
                        + "boolean expression are a flat one-shot list — the enum overhead "
                        + "(values(), constructor, holder, lookup) doesn't pay back here.");
    }

    @Test
    void stillFiresWhenConstantsAreReferencedFromMultipleSites() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Multi {
                    private static final String SLF4J_TRACE = "trace";
                    private static final String SLF4J_DEBUG = "debug";
                    private static final String SLF4J_INFO = "info";
                    boolean foo(final String name) { return SLF4J_TRACE.equals(name); }
                    boolean bar(final String name) { return SLF4J_DEBUG.equals(name); }
                    boolean baz(final String name) { return SLF4J_INFO.equals(name); }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "J3 still fires when references are scattered across multiple methods — "
                        + "that's an enum-shape access pattern.");
    }

    @Test
    void stillFiresOnPublicConstants() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Pub {
                    public static final int STATUS_ACTIVE = 1;
                    public static final int STATUS_INACTIVE = 2;
                    public static final int STATUS_PENDING = 3;
                    int dispatch(final int s) {
                        return s == STATUS_ACTIVE ? 1 : s == STATUS_INACTIVE ? 2 : STATUS_PENDING;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "J3 still fires on public/protected constants — they're an API surface "
                        + "users iterate over, exactly the case enums solve.");
    }

    @Test
    void ignoresFewConstants() {
        final var recipe = new EnumForConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    static final int STATUS_ACTIVE = 1;
                    static final int STATUS_INACTIVE = 2;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
