package org.fiftieshousewife.cleancode.recipes;

import org.fiftieshousewife.cleancode.recipes.FlagArgumentRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlagArgumentRecipeTest {

    @Test
    void detectsBooleanParamOnPublicMethod() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void doStuff(String name, boolean verbose) {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("doStuff", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("verbose", recipe.collectedRows().getFirst().paramName())
        );
    }

    @Test
    void detectsBooleanParamOnPackagePrivateMethod() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void packageMethod(boolean dryRun) {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("dryRun", recipe.collectedRows().getFirst().paramName())
        );
    }

    @Test
    void ignoresBooleanParamOnPrivateMethod() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private void hidden(boolean flag) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresConstructorBooleanParams() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public Foo(boolean init) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void noFalsePositivesOnNonBooleanParams() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void noFlags(String name, int count) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsProtectedMethodBooleanParam() {
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    protected void guarded(boolean enabled) {}
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }
}
