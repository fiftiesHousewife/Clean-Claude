package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureEnvyRecipeTest {

    private static final String BAR_CLASS = """
            package com.example;
            public class Bar {
                public void doA() {}
                public void doB() {}
                public void doC() {}
            }
            """;

    @Test
    void detectsMethodEnvyingAnotherProjectClass() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                BAR_CLASS,
                """
                package com.example;
                public class Foo {
                    Bar bar;
                    void process() {
                        bar.doA();
                        bar.doB();
                        bar.doC();
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("process", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals("Bar", recipe.collectedRows().getFirst().enviedClass())
        );
    }

    @Test
    void ignoresCallsOnJdkTypes() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process(String input) {
                        input.length();
                        input.trim();
                        input.toLowerCase();
                        input.toUpperCase();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithMoreSelfCalls() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                BAR_CLASS,
                """
                package com.example;
                public class Foo {
                    Bar bar;
                    void doX() {}
                    void doY() {}
                    void doZ() {}
                    void balanced() {
                        doX();
                        doY();
                        doZ();
                        bar.doA();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWithFewerThanThreeCalls() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                BAR_CLASS,
                """
                package com.example;
                public class Foo {
                    Bar bar;
                    void tiny() {
                        bar.doA();
                        bar.doB();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresStaticMethodsAndConstructors() {
        final var recipe = new FeatureEnvyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                BAR_CLASS,
                """
                package com.example;
                public class Foo {
                    static Bar bar;
                    static void process() {
                        bar.doA();
                        bar.doB();
                        bar.doC();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
