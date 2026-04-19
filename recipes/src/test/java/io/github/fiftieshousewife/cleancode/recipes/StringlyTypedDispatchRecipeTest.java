package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringlyTypedDispatchRecipeTest {

    @Test
    void detectsSwitchOnStringParameter() {
        final var recipe = new StringlyTypedDispatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process(String type) {
                        switch (type) {
                            case "A": break;
                            case "B": break;
                            case "C": break;
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("type", recipe.collectedRows().getFirst().parameterName()),
                () -> assertTrue(recipe.collectedRows().getFirst().branchCount() >= 2)
        );
    }

    @Test
    void detectsIfElseChainWithEquals() {
        final var recipe = new StringlyTypedDispatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process(String kind) {
                        if ("A".equals(kind)) {
                            int x = 1;
                        } else if ("B".equals(kind)) {
                            int x = 2;
                        } else if ("C".equals(kind)) {
                            int x = 3;
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("kind", recipe.collectedRows().getFirst().parameterName()),
                () -> assertTrue(recipe.collectedRows().getFirst().branchCount() >= 2)
        );
    }

    @Test
    void ignoresMethodWithoutStringParameters() {
        final var recipe = new StringlyTypedDispatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public void process(int type) {
                        switch (type) {
                            case 1: break;
                            case 2: break;
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresStringParamNotUsedForDispatch() {
        final var recipe = new StringlyTypedDispatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String format(String value) {
                        return value.trim();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresEqualsMethod() {
        final var recipe = new StringlyTypedDispatchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public boolean equals(String other) {
                        if ("A".equals(other)) {
                            return true;
                        } else if ("B".equals(other)) {
                            return true;
                        }
                        return false;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
