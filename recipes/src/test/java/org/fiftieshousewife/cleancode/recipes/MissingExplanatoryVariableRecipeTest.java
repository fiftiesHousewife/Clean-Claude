package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingExplanatoryVariableRecipeTest {

    @Test
    void detectsDeeplyChainedMethodCallInArgument() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getX().transform().serialize());
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresShortChainedMethodCallInArgument() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getX().transform());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsComplexBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int compute(int a, int b, int c, int d, int e) {
                        return a + b * c - d + e;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("compute", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresChainedArgumentWhenOuterCallIsAlreadyExtractedToVariable() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    Object compute(Object x) { return x; }
                    void use(Object x) {}
                    void process() {
                        final var result = compute(bar.getX().transform().serialize());
                        use(result);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "compute(...) is already the initializer of a var decl; do not re-flag its chained argument");
    }

    @Test
    void ignoresSimpleBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int compute(int a, int b, int c, int d) {
                        return a + b * c - d;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
