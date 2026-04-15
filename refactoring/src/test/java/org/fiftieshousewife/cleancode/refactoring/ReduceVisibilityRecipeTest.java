package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReduceVisibilityRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReduceVisibilityRecipe(5));
    }

    @Test
    void removesPrivateFromMethodAboveThreshold() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private int calculate(int a, int b) {
                                int sum = a + b;
                                int product = a * b;
                                int diff = a - b;
                                int result = sum + product + diff;
                                return result;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            int calculate(int a, int b) {
                                int sum = a + b;
                                int product = a * b;
                                int diff = a - b;
                                int result = sum + product + diff;
                                return result;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesShortPrivateMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private int add(int a, int b) {
                                return a + b;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesPublicMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public int calculate(int a, int b) {
                                int sum = a + b;
                                int product = a * b;
                                int diff = a - b;
                                int result = sum + product + diff;
                                return result;
                            }
                        }
                        """
                )
        );
    }
}
