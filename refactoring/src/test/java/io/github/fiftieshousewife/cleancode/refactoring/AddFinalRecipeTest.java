package io.github.fiftieshousewife.cleancode.refactoring;

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

    @Test
    void leavesLambdaParametersAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            void each(List<String> xs) {
                                xs.forEach(x -> System.out.println(x));
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            void each(final List<String> xs) {
                                xs.forEach(x -> System.out.println(x));
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodReferenceArgumentsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            void each(List<String> xs) {
                                xs.forEach(System.out::println);
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            void each(final List<String> xs) {
                                xs.forEach(System.out::println);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void addsFinalBetweenAnnotationAndTypeOnMethodParameter() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public Foo(@SuppressWarnings("unused") int value) {
                                int x = value;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            public Foo(@SuppressWarnings("unused") final int value) {
                                final int x = value;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void addsFinalWhenNoModifiersOrAnnotationsOnParameter() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method(int value) {
                                int x = value;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            void method(final int value) {
                                final int x = value;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesCatchParameterAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                try {
                                    System.out.println("hi");
                                } catch (RuntimeException e) {
                                    System.out.println(e.getMessage());
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesTryWithResourcesVariableAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.StringReader;
                        public class Foo {
                            int count(String s) throws Exception {
                                try (StringReader r = new StringReader(s)) {
                                    return r.read();
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.io.StringReader;
                        public class Foo {
                            int count(final String s) throws Exception {
                                try (StringReader r = new StringReader(s)) {
                                    return r.read();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForEachLoopVariableAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            int sum(List<Integer> xs) {
                                int total = 0;
                                for (int x : xs) {
                                    total += x;
                                }
                                return total;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            int sum(final List<Integer> xs) {
                                int total = 0;
                                for (int x : xs) {
                                    total += x;
                                }
                                return total;
                            }
                        }
                        """
                )
        );
    }
}
