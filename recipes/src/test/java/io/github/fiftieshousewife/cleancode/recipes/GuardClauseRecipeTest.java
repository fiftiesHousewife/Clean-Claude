package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardClauseRecipeTest {

    @Test
    void ignoresMultipleIfContinueInLoopWithSameShape() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void process(List<String> items) {
                        for (String item : items) {
                            if (item == null) {
                                continue;
                            }
                            if (item.isEmpty()) {
                                continue;
                            }
                            System.out.println(item);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G30.1: two `continue;` guards are one composite precondition (skip-if-invalid), "
                        + "not two distinct behaviours.");
    }

    @Test
    void ignoresThreeSameShapeReturnGuards() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process(String input) {
                        if (input == null) {
                            return;
                        }
                        if (input.isEmpty()) {
                            return;
                        }
                        if (input.isBlank()) {
                            return;
                        }
                        System.out.println(input);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G30.1: three identical `return;` guards collapse to one composite precondition.");
    }

    @Test
    void ignoresPeelThrowableShape() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Optional;
                public class Visitor {
                    Optional<String> peelThrowableFromConcat(final Object method) {
                        if (method == null) {
                            return Optional.empty();
                        }
                        if (!(method instanceof String s)) {
                            return Optional.empty();
                        }
                        if (s.isBlank()) {
                            return Optional.empty();
                        }
                        return Optional.of(s);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G30.1: three `return Optional.empty();` guards collapse to one — "
                        + "ConcatThrowableMessageVisitor.peelThrowableFromConcat shape from "
                        + "CLEANCODE_PLUGIN_FEEDBACK.md.");
    }

    @Test
    void firesOnDistinctShapeGuards() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int process(String a, Integer b, Object c) {
                        if (a == null) {
                            throw new NullPointerException();
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (c == null) {
                            return 0;
                        }
                        return 1;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "G30 still fires when guards have distinct exit shapes — "
                        + "throw vs `return -1` vs `return 0` is three behaviours.");
    }

    @Test
    void ignoresSingleGuardClause() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process(String input) {
                        if (input == null) {
                            return;
                        }
                        System.out.println(input);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresIfWithElse() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process(String input) {
                        if (input == null) {
                            System.out.println("null");
                        } else {
                            System.out.println(input);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
