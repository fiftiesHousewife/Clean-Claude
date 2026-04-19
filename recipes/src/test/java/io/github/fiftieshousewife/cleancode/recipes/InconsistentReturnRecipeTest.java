package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InconsistentReturnRecipeTest {

    @Test
    void detectsMixOfVoidMutatorsAndReturningMethods() {
        final var recipe = new InconsistentReturnRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;
                public class Foo {
                    List<String> buildItems() {
                        List<String> items = new ArrayList<>();
                        items.add("a");
                        return items;
                    }
                    void addSpecialItems(List<String> items) {
                        items.add("special");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresConsistentReturningMethods() {
        final var recipe = new InconsistentReturnRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;
                public class Foo {
                    List<String> buildItems() {
                        List<String> items = new ArrayList<>();
                        items.add("a");
                        return items;
                    }
                    List<String> buildOtherItems() {
                        List<String> items = new ArrayList<>();
                        items.add("b");
                        return items;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
