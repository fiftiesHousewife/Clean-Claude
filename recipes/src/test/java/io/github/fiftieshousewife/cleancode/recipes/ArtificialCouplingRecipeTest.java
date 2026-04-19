package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtificialCouplingRecipeTest {

    @Test
    void detectsConstantUsedOnlyInAnotherClass() {
        final var recipe = new ArtificialCouplingRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class UserController {
                    public static final int MAX_RETRIES = 3;
                    void handleRequest() {
                        System.out.println("handling");
                    }
                }
                """,
                """
                package com.example;
                public class EmailService {
                    void send() {
                        for (int i = 0; i < UserController.MAX_RETRIES; i++) {
                            System.out.println("sending");
                        }
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("UserController", recipe.collectedRows().getFirst().declaringClass()),
                () -> assertEquals("MAX_RETRIES", recipe.collectedRows().getFirst().constantName()),
                () -> assertEquals("EmailService", recipe.collectedRows().getFirst().usedInClass())
        );
    }

    @Test
    void ignoresConstantUsedInSameClass() {
        final var recipe = new ArtificialCouplingRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class EmailService {
                    private static final int MAX_RETRIES = 3;
                    void send() {
                        for (int i = 0; i < MAX_RETRIES; i++) {
                            System.out.println("sending");
                        }
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNonStaticFields() {
        final var recipe = new ArtificialCouplingRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class Config {
                    public final String name = "test";
                }
                """,
                """
                package com.example;
                public class Service {
                    void use(Config c) {
                        System.out.println(c.name);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
