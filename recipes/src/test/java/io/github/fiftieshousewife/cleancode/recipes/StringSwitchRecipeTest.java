package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringSwitchRecipeTest {

    @Test
    void detectsSwitchExpressionOnStringWithThreeOrMoreCases() {
        final var recipe = new StringSwitchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Converter {
                    public int convert(String xmlSeverity) {
                        return switch (xmlSeverity) {
                            case "error" -> 1;
                            case "info" -> 2;
                            case "warning" -> 3;
                            default -> 0;
                        };
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Converter", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("convert", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals(4, recipe.collectedRows().getFirst().caseCount()),
                () -> assertEquals("xmlSeverity", recipe.collectedRows().getFirst().selectorName())
        );
    }

    @Test
    void detectsSwitchStatementOnStringWithThreeOrMoreCases() {
        final var recipe = new StringSwitchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Dispatcher {
                    public void dispatch(String command) {
                        switch (command) {
                            case "start":
                                System.out.println("starting");
                                break;
                            case "stop":
                                System.out.println("stopping");
                                break;
                            case "restart":
                                System.out.println("restarting");
                                break;
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Dispatcher", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("dispatch", recipe.collectedRows().getFirst().methodName()),
                () -> assertEquals(3, recipe.collectedRows().getFirst().caseCount()),
                () -> assertEquals("command", recipe.collectedRows().getFirst().selectorName())
        );
    }

    @Test
    void ignoresSwitchOnIntOrEnum() {
        final var recipe = new StringSwitchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class NonStringSwitch {
                    public String fromInt(int code) {
                        return switch (code) {
                            case 1 -> "one";
                            case 2 -> "two";
                            case 3 -> "three";
                            default -> "unknown";
                        };
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresSwitchWithFewerThanThreeCases() {
        final var recipe = new StringSwitchRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class SmallSwitch {
                    public int twoCase(String value) {
                        return switch (value) {
                            case "yes" -> 1;
                            default -> 0;
                        };
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
