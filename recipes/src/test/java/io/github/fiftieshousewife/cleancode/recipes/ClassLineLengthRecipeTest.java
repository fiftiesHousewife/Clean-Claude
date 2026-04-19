package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.ClassLineLengthRecipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassLineLengthRecipeTest {

    @Test
    void detectsClassExceedingThreshold() {
        final var recipe = new ClassLineLengthRecipe();
        final String longClass = generateClassWithLines(200);
        RecipeTestHelper.runAgainst(recipe, longClass);

        assertEquals(1, recipe.collectedRows().size());
        assertTrue(recipe.collectedRows().getFirst().lineCount() > 150);
    }

    @Test
    void ignoresClassUnderThreshold() {
        final var recipe = new ClassLineLengthRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Small {
                    void method() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    private String generateClassWithLines(int targetLines) {
        final var sb = new StringBuilder();
        sb.append("package com.example;\npublic class Big {\n");
        for (int i = 0; i < targetLines - 3; i++) {
            sb.append("    int field").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
