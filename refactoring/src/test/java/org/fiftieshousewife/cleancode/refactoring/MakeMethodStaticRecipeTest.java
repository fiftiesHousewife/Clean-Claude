package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MakeMethodStaticRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MakeMethodStaticRecipe());
    }

    @Test
    void addsStaticToMethodThatUsesNoInstanceState() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public int doubleIt(int x) {
                                return x * 2;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            public static int doubleIt(int x) {
                                return x * 2;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodUsingInstanceFieldAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Counter {
                            private int count = 0;
                            public int next() {
                                count++;
                                return count;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodThatReadsThisFieldViaExplicitThis() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Bar {
                            private final String name = "x";
                            public String greet() {
                                return "hi " + this.name;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodThatCallsSiblingInstanceMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Baz {
                            public int combine() {
                                return helper() + 1;
                            }
                            public int helper() {
                                return counter;
                            }
                            private int counter = 0;
                        }
                        """
                )
        );
    }

    @Test
    void addsStaticToMethodThatCallsOnlyStaticSibling() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Qux {
                            public int combined(int x) {
                                return add(x, 1);
                            }
                            public static int add(int a, int b) {
                                return a + b;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Qux {
                            public static int combined(int x) {
                                return add(x, 1);
                            }
                            public static int add(int a, int b) {
                                return a + b;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesConstructorAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class C {
                            public C() {
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesAlreadyStaticMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class D {
                            public static int square(int x) {
                                return x * x;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesAbstractMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public abstract class E {
                            public abstract int unknown();
                        }
                        """
                )
        );
    }

    @Test
    void insertsStaticAfterVisibilityModifier() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class G {
                            private int identity(int x) {
                                return x;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class G {
                            private static int identity(int x) {
                                return x;
                            }
                        }
                        """
                )
        );
    }
}
