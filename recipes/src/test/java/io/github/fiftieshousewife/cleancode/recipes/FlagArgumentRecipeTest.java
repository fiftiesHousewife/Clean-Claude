package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.FlagArgumentRecipe;
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
    void ignoresBooleanParamOnPackagePrivateMethod() {
        // F3 is about API design — internal helpers (package-private)
        // shouldn't fire because passing a flag through a chain of
        // internal overloads is a different concern.
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void packageMethod(boolean dryRun) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
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
    void ignoresProtectedMethodBooleanParam() {
        // protected is reachable from subclasses but isn't part of the
        // module's public API — F3 is API-design noise here.
        final var recipe = new FlagArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    protected void guarded(boolean enabled) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
