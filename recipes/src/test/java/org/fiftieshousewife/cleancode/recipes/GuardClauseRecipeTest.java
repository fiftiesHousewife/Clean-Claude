package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardClauseRecipeTest {

    @Test
    void detectsMultipleIfContinueInLoop() {
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

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void detectsIfReturnGuard() {
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

        assertEquals(1, recipe.collectedRows().size());
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
