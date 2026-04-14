package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InheritConstantsRecipeTest {

    @Test
    void detectsClassImplementingConstantOnlyInterface() {
        final var recipe = new InheritConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public interface StatusCodes {
                    int ACTIVE = 1;
                    int INACTIVE = 2;
                }
                """, """
                package com.example;
                public class OrderService implements StatusCodes {
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("OrderService", recipe.collectedRows().getFirst().className());
        assertEquals("StatusCodes", recipe.collectedRows().getFirst().interfaceName());
    }

    @Test
    void ignoresClassImplementingBehaviourInterface() {
        final var recipe = new InheritConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public interface FindingSource {
                    String findAll();
                }
                """, """
                package com.example;
                public class OrderService implements FindingSource {
                    public String findAll() { return ""; }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresInterfaceWithMethodsAndConstants() {
        final var recipe = new InheritConstantsRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public interface Configurable {
                    int DEFAULT_TIMEOUT = 30;
                    void configure();
                }
                """, """
                package com.example;
                public class Server implements Configurable {
                    public void configure() {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
