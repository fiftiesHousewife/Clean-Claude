package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingExplanatoryVariableRecipeTest {

    @Test
    void detectsDeeplyChainedMethodCallWithIntermediateArguments() {
        // The G19 smell is hidden complexity in an expression — typically
        // a stream-style pipeline whose intermediate steps deserve names.
        // A chain with arg-taking intermediate calls (filter, map, reduce)
        // qualifies because each step is computing something.
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.stream.Collectors;
                public class Foo {
                    List<String> bar;
                    void process() {
                        System.out.println(bar.stream().filter(s -> s.length() > 0).collect(Collectors.joining(",")));
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "stream().filter(...).collect(...) has intermediate arg-taking calls — fire");
    }

    @Test
    void ignoresPureGetterChainBecauseEachSegmentIsNavigationNotComputation() {
        // bar.getX().transform().serialize() and Gradle-style
        // reportFile.get().getAsFile().toPath() are deep but trivial:
        // every segment is a no-arg accessor / path step. Naming the
        // intermediates wouldn't add information; the chain reads as
        // a path expression. The G19 fix doesn't apply.
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

        assertTrue(recipe.collectedRows().isEmpty(),
                "no-arg navigation chain is not the G19 smell");
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
