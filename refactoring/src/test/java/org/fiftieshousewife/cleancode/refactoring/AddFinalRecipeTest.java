package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddFinalRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddFinalRecipe());
    }

    @Test
    void addsFinalToNonReassignedLocal() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                String name = "hello";
                                System.out.println(name);
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                final String name = "hello";
                                System.out.println(name);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToReassignedVariable() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                String name = "hello";
                                name = "world";
                                System.out.println(name);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToAlreadyFinalVariable() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                final String name = "hello";
                                System.out.println(name);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void addsFinalToMultipleVariablesInSameMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                String a = "hello";
                                int b = 42;
                                System.out.println(a + b);
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                final String a = "hello";
                                final int b = 42;
                                System.out.println(a + b);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToIncrementedVariable() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                int count = 0;
                                count++;
                                System.out.println(count);
                            }
                        }
                        """
                )
        );
    }
}
