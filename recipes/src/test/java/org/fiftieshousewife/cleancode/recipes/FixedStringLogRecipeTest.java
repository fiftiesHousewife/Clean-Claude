package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStringLogRecipeTest {

    @Test
    void flagsLogCallWithSingleStringLiteralArgument() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger log = LoggerFactory.getLogger(Service.class);
                    public void start() {
                        log.info("starting up");
                    }
                }
                """);
        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Service", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("info", recipe.collectedRows().getFirst().level()),
                () -> assertEquals("starting up", recipe.collectedRows().getFirst().literal()));
    }

    @Test
    void flagsAllStandardSlf4jLevels() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger log = LoggerFactory.getLogger(Service.class);
                    public void noisy() {
                        log.trace("traced");
                        log.debug("debugged");
                        log.info("informed");
                        log.warn("warned");
                        log.error("errored");
                    }
                }
                """);
        assertEquals(5, recipe.collectedRows().size());
    }

    @Test
    void ignoresLogCallWithFormatPlaceholderAndArgument() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger log = LoggerFactory.getLogger(Service.class);
                    public void start(String userId) {
                        log.info("user {} starting up", userId);
                    }
                }
                """);
        assertTrue(recipe.collectedRows().isEmpty(),
                "two-arg log calls have variable content — not clutter");
    }

    @Test
    void ignoresLogCallWithStringConcatenationContainingVariable() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger log = LoggerFactory.getLogger(Service.class);
                    public void start(int count) {
                        log.info("count: " + count);
                    }
                }
                """);
        assertTrue(recipe.collectedRows().isEmpty(),
                "string concat with a non-literal piece carries information — not clutter");
    }

    @Test
    void flagsLogCallWithConcatenationOfOnlyLiterals() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger log = LoggerFactory.getLogger(Service.class);
                    public void start() {
                        log.info("starting" + " up");
                    }
                }
                """);
        assertEquals(1, recipe.collectedRows().size(),
                "concatenation of two literals is still a constant — clutter");
    }

    @Test
    void recognisesAlternativeLoggerFieldNames() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                public class Service {
                    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
                    private static final Logger LOG = LoggerFactory.getLogger(Service.class);
                    public void run() {
                        LOGGER.info("a");
                        LOG.warn("b");
                    }
                }
                """);
        assertEquals(2, recipe.collectedRows().size(),
                "LOGGER and LOG are conventional logger field names");
    }

    @Test
    void ignoresMethodCallsThatLookLikeLogsButArentLoggers() {
        final var recipe = new FixedStringLogRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Service {
                    private final StringBuilder buffer = new StringBuilder();
                    public void run() {
                        buffer.append("info");
                    }
                    public void info(String msg) { }
                    public void wrap() {
                        info("hello");
                    }
                }
                """);
        assertTrue(recipe.collectedRows().isEmpty(),
                "an info() method on `this` (no logger receiver) is not a log call");
    }
}
