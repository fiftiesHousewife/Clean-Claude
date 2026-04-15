package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MakeStaticRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MakeStaticRecipe());
    }

    @Test
    void addsStaticToMethodNotUsingInstanceState() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public int add(int a, int b) {
                                int sum = a + b;
                                int doubled = sum * 2;
                                return doubled;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            public static int add(int a, int b) {
                                int sum = a + b;
                                int doubled = sum * 2;
                                return doubled;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodUsingInstanceField() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private String name;
                            public String greet() {
                                String greeting = "Hello";
                                String full = greeting + " " + this.name;
                                return full;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesOverriddenMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            @Override
                            public String toString() {
                                String result = "Foo";
                                String padded = result + "!";
                                return padded;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesPrivateMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private int add(int a, int b) {
                                int sum = a + b;
                                int doubled = sum * 2;
                                return doubled;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesAlreadyStaticMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public static int add(int a, int b) {
                                int sum = a + b;
                                int doubled = sum * 2;
                                return doubled;
                            }
                        }
                        """
                )
        );
    }
}
