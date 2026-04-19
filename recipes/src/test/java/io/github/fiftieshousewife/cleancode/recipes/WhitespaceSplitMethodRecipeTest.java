package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhitespaceSplitMethodRecipeTest {

    @Test
    void detectsMethodWithMultipleBlankLineSections() {
        final var recipe = new WhitespaceSplitMethodRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class OrderService {
                    void processOrder() {
                        int x = 1;
                        int y = 2;

                        int z = x + y;
                        int w = z * 2;

                        int result = w + 1;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertTrue(recipe.collectedRows().getFirst().blankLineCount() >= 2)
        );
    }

    @Test
    void ignoresMethodWithNoBlankLines() {
        final var recipe = new WhitespaceSplitMethodRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Clean {
                    void compact() {
                        int x = 1;
                        int y = 2;
                        int z = x + y;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithOnlyOneBlankLine() {
        final var recipe = new WhitespaceSplitMethodRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Acceptable {
                    void slightlySeparated() {
                        int x = 1;

                        int y = x + 1;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void includesCorrectMethodAndClassNameInRow() {
        final var recipe = new WhitespaceSplitMethodRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class ReportGenerator {
                    void buildReport() {
                        int a = 1;

                        int b = 2;

                        int c = 3;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("ReportGenerator", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("buildReport", recipe.collectedRows().getFirst().methodName())
        );
    }
}
