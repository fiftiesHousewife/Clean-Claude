package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseClassDependencyRecipeTest {

    @Test
    void detectsBaseClassImportingDerivative() {
        final var recipe = new BaseClassDependencyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class Animal {
                    void speak() {
                        if (this instanceof Dog) {
                            System.out.println("woof");
                        }
                    }
                }
                """,
                """
                package com.example;
                public class Dog extends Animal {
                    void fetch() {
                        System.out.println("fetching");
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Animal", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("Dog", recipe.collectedRows().getFirst().derivativeName())
        );
    }

    @Test
    void ignoresClassWithNoDerivativeReferences() {
        final var recipe = new BaseClassDependencyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class Animal {
                    void speak() {
                        System.out.println("...");
                    }
                }
                """,
                """
                package com.example;
                public class Dog extends Animal {
                    void fetch() {
                        System.out.println("fetching");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsInstanceofCheckForSubclass() {
        final var recipe = new BaseClassDependencyRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class Shape {
                    String describe() {
                        if (this instanceof Circle) {
                            return "round";
                        }
                        return "shape";
                    }
                }
                """,
                """
                package com.example;
                public class Circle extends Shape {
                    double radius;
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Shape", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("Circle", recipe.collectedRows().getFirst().derivativeName())
        );
    }
}
