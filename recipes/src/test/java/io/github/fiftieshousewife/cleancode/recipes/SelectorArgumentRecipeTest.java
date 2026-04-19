package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectorArgumentRecipeTest {

    @Test
    void detectsEnumParameterUsedInSwitch() {
        final var recipe = new SelectorArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    enum Mode { FAST, SLOW }
                    void process(Mode mode) {
                        switch (mode) {
                            case FAST: System.out.println("fast"); break;
                            case SLOW: System.out.println("slow"); break;
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("mode", recipe.collectedRows().getFirst().parameterName())
        );
    }

    @Test
    void detectsBooleanParameterUsedInIf() {
        final var recipe = new SelectorArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void render(boolean html) {
                        if (html) {
                            System.out.println("<p>hello</p>");
                        } else {
                            System.out.println("hello");
                        }
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresParameterNotUsedAsSelector() {
        final var recipe = new SelectorArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void greet(String name) {
                        System.out.println("Hello " + name);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
