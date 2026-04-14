package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImperativeLoopRecipeTest {

    @Test
    void detectsForEachWithAdd() {
        final var recipe = new ImperativeLoopRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;
                public class Foo {
                    void collect(List<String> source) {
                        List<String> result = new ArrayList<>();
                        for (String item : source) {
                            result.add(item);
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("collect", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("for-each with add", recipe.collectedRows().getFirst().loopPattern())
        );
    }

    @Test
    void detectsForEachWithFilterAdd() {
        final var recipe = new ImperativeLoopRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;
                public class Foo {
                    void filterAndCollect(List<String> source) {
                        List<String> result = new ArrayList<>();
                        for (String item : source) {
                            if (item.length() > 3) {
                                result.add(item);
                            }
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("for-each with filter-add", recipe.collectedRows().getFirst().loopPattern())
        );
    }

    @Test
    void ignoresLoopWithReturn() {
        final var recipe = new ImperativeLoopRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    String findFirst(List<String> source) {
                        for (String item : source) {
                            if (item.length() > 3) {
                                return item;
                            }
                        }
                        return null;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresLoopWithMultipleStatements() {
        final var recipe = new ImperativeLoopRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;
                public class Foo {
                    void process(List<String> source) {
                        List<String> result = new ArrayList<>();
                        for (String item : source) {
                            String upper = item.toUpperCase();
                            result.add(upper);
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
