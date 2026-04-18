package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CollapseSiblingGuardsRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new CollapseSiblingGuardsRecipe());
    }

    @Test
    void collapsesTwoSameBodyReturnNullGuards() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            String lookup(String token) {
                                if (token == null) return null;
                                if (token.isBlank()) return null;
                                return token.trim();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            String lookup(String token) {
                                if (token == null || token.isBlank()) return null;
                                return token.trim();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void collapsesThreeConsecutiveGuards() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Bar {
                            int count(String s) {
                                if (s == null) return 0;
                                if (s.isEmpty()) return 0;
                                if (s.isBlank()) return 0;
                                return s.length();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Bar {
                            int count(String s) {
                                if (s == null || s.isEmpty() || s.isBlank()) return 0;
                                return s.length();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesGuardsWithDifferentBodiesAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Baz {
                            String label(int x) {
                                if (x < 0) return "neg";
                                if (x > 0) return "pos";
                                return "zero";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesSingleGuardAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Single {
                            String take(String x) {
                                if (x == null) return null;
                                return x.trim();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void collapsesContinueGuardsInLoopBody() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Loops {
                            int sumPositive(List<Integer> xs) {
                                int total = 0;
                                for (Integer x : xs) {
                                    if (x == null) continue;
                                    if (x < 0) continue;
                                    total += x;
                                }
                                return total;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        public class Loops {
                            int sumPositive(List<Integer> xs) {
                                int total = 0;
                                for (Integer x : xs) {
                                    if (x == null || x < 0) continue;
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
