package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Set;

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
    void leavesOverrideMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Child extends Object {
                            @Override
                            public String toString() {
                                return "child";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesJUnitTestMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.junit.jupiter.api.Test;
                        class FooTest {
                            @Test
                            void addsTwoAndTwo() {
                                int result = 2 + 2;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesJUnitLifecycleMethodsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.junit.jupiter.api.BeforeEach;
                        import org.junit.jupiter.api.AfterEach;
                        class LifecycleTest {
                            @BeforeEach
                            void setUp() {
                                int x = 1;
                            }
                            @AfterEach
                            void tearDown() {
                                int y = 2;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesNestedOverrideFromAnonymousClass() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Host {
                            Runnable task() {
                                return new Runnable() {
                                    @Override
                                    public void run() {
                                        int n = 1;
                                    }
                                };
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Host {
                            static Runnable task() {
                                return new Runnable() {
                                    @Override
                                    public void run() {
                                        int n = 1;
                                    }
                                };
                            }
                        }
                        """
                )
        );
    }

    @Test
    void insertsStaticBeforeReturnTypeWhenNoVisibilityModifier() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class H {
                            int identity(int x) {
                                return x;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class H {
                            static int identity(int x) {
                                return x;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesInterfaceDefaultMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public interface Hasher {
                            int hash(String s);
                            default boolean accepts(String s) {
                                return true;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesInterfaceAbstractMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public interface Signer {
                            byte[] sign(byte[] payload);
                        }
                        """
                )
        );
    }

    @Test
    void leavesInterfacePrivateHelperMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public interface Gate {
                            default boolean ok() {
                                return helper();
                            }
                            private boolean helper() {
                                return true;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesRecordCompactMethodAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public record Sum(int a, int b) {
                            public int total() {
                                return a + b;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesAnnotationAttributesAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public @interface Note {
                            String value() default "";
                            int priority() default 0;
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodCallingInheritedGetClassAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Named {
                            public String describe() {
                                return getClass().getSimpleName();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodCallingInheritedHashCodeAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Hasher {
                            public int quickHash() {
                                return hashCode() * 31;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodCallingInheritedToStringAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Labelled {
                            public String describe() {
                                return "[" + toString() + "]";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodReferencingBareThisAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Link {
                            public Link self() {
                                return this;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodPassingThisAsArgumentAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.function.Consumer;
                        public class Self {
                            public void register(Consumer<Self> sink) {
                                sink.accept(this);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesSuperCallAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Child extends Parent {
                            public String describe() {
                                return super.describe() + " child";
                            }
                        }
                        class Parent {
                            public String describe() {
                                return "parent";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesExplicitThisMethodCallAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Wrap {
                            private final String name = "x";
                            public String label() {
                                return this.name;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesThisMethodReferenceAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.function.Supplier;
                        public class Factory {
                            public Supplier<Factory> supplier() {
                                return this::self;
                            }
                            public Factory self() {
                                return this;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodInstantiatingNonStaticInnerClassAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Outer {
                            private String label = "x";
                            class Inner {
                                String tag() {
                                    return label;
                                }
                            }
                            public Inner make() {
                                return new Inner();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesMethodWithAnonymousClassCapturingOuterFieldAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Host {
                            private final String label = "x";
                            public Runnable task() {
                                return new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println(label);
                                    }
                                };
                            }
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

    @Test
    void leavesMethodProtectedByExternalSuperCallSetAlone() {
        rewriteRun(
                spec -> spec.recipe(new MakeMethodStaticRecipe(Set.of("describe"))),
                java(
                        """
                        package com.example;
                        public class Parent {
                            public String describe() {
                                return "parent";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void emptyExternalSuperCallSetBehavesLikeDefault() {
        rewriteRun(
                spec -> spec.recipe(new MakeMethodStaticRecipe(Set.of())),
                java(
                        """
                        package com.example;
                        public class Standalone {
                            public int doubleIt(int x) {
                                return x * 2;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Standalone {
                            public static int doubleIt(int x) {
                                return x * 2;
                            }
                        }
                        """
                )
        );
    }
}
