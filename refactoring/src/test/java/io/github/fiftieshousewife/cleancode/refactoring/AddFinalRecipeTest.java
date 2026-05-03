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
    void addsFinalToMethodParameterInOrdinaryClass() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method(int value) {
                                System.out.println(value);
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            void method(final int value) {
                                System.out.println(value);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesOrdinaryLocalVariableAloneInNonSealedClass() {
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
                        """
                )
        );
    }

    @Test
    void addsFinalToLocalVariableInsideSealedClass() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public sealed class Foo permits Foo.Bar {
                            void method() {
                                String name = "hello";
                                System.out.println(name);
                            }
                            public static final class Bar extends Foo {}
                        }
                        """,
                        """
                        package com.example;
                        public sealed class Foo permits Foo.Bar {
                            void method() {
                                final String name = "hello";
                                System.out.println(name);
                            }
                            public static final class Bar extends Foo {}
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToReassignedParameter() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method(int value) {
                                value = value + 1;
                                System.out.println(value);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToAlreadyFinalParameter() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method(final String name) {
                                System.out.println(name);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotAddFinalToIncrementedParameter() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method(int count) {
                                count++;
                                System.out.println(count);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesLambdaParametersAloneEvenInsideSealedClass() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public sealed class Foo permits Foo.Bar {
                            void each(List<String> xs) {
                                xs.forEach(x -> System.out.println(x));
                            }
                            public static final class Bar extends Foo {}
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        public sealed class Foo permits Foo.Bar {
                            void each(final List<String> xs) {
                                xs.forEach(x -> System.out.println(x));
                            }
                            public static final class Bar extends Foo {}
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
                                System.out.println(value);
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            public Foo(@SuppressWarnings("unused") final int value) {
                                System.out.println(value);
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
