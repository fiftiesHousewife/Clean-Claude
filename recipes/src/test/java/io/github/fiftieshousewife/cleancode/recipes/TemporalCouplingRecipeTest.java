package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemporalCouplingRecipeTest {

    @Test
    void detectsSequenceOfVoidCallsWithNoDependency() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void setup() {
                        init();
                        configure();
                        start();
                    }
                    void init() {}
                    void configure() {}
                    void start() {}
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("setup", recipe.collectedRows().getFirst().methodName()),
                () -> assertTrue(recipe.collectedRows().getFirst().callCount() >= 3)
        );
    }

    @Test
    void ignoresMethodsWithReturnValueChaining() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void process() {
                        String a = load();
                        String b = transform(a);
                        save(b);
                    }
                    String load() { return ""; }
                    String transform(String s) { return s; }
                    void save(String s) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresFewConsecutiveCalls() {
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void setup() {
                        init();
                        start();
                    }
                    void init() {}
                    void start() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresRegistrationLoopBecauseTheCallsAreNotDistinct() {
        // The same method called repeatedly with different arguments —
        // e.g. plugin registration, listener wiring — is a registration
        // loop, not temporal coupling. Order between identical calls
        // doesn't matter, so the smell does not apply.
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.ArrayList;
                import java.util.List;
                public class Foo {
                    void register() {
                        List<String> plugins = new ArrayList<>();
                        plugins.add("a");
                        plugins.add("b");
                        plugins.add("c");
                        plugins.add("d");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "consecutive calls of the same method (different args) "
                        + "are a registration loop, not temporal coupling");
    }

    @Test
    void stillDetectsCouplingWhenSomeCallsRepeat() {
        // A run that mixes a repeating call with distinct ones still
        // hits the threshold on distinct count. Three distinct
        // (receiver, method) pairs → fires.
        final var recipe = new TemporalCouplingRecipe(3);
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void setup() {
                        register("a");
                        register("b");
                        configure();
                        start();
                        validate();
                    }
                    void register(String s) {}
                    void configure() {}
                    void start() {}
                    void validate() {}
                }
                """);

        assertFalse(recipe.collectedRows().isEmpty(),
                "register/register collapses but configure/start/validate are 3 distinct calls");
    }
}
