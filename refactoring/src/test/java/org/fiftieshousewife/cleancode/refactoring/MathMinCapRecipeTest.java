package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MathMinCapRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MathMinCapRecipe());
    }

    @Test
    void replacesIfGreaterThanCapWithMathMin() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Cap {
                            public long clamp(long delayMs) {
                                if (delayMs > 10000) {
                                    delayMs = 10000;
                                }
                                return delayMs;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Cap {
                            public long clamp(long delayMs) {
                                delayMs = Math.min(delayMs, 10000);
                                return delayMs;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void replacesIfLessThanFloorWithMathMax() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Floor {
                            public int floorAt(int n) {
                                if (n < 0) {
                                    n = 0;
                                }
                                return n;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Floor {
                            public int floorAt(int n) {
                                n = Math.max(n, 0);
                                return n;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void handlesSingleLineBody() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Single {
                            public int clamp(int v) {
                                if (v > 100) v = 100;
                                return v;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Single {
                            public int clamp(int v) {
                                v = Math.min(v, 100);
                                return v;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesIfWithElseBranchAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Else {
                            public int clamp(int v) {
                                if (v > 100) {
                                    v = 100;
                                } else {
                                    v = v + 1;
                                }
                                return v;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesIfWithNonMatchingAssignmentAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Other {
                            public int clamp(int v, int other) {
                                if (v > 100) {
                                    other = 100;
                                }
                                return other;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesIfWithMultipleStatementsInBodyAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class MultiStmt {
                            public int clamp(int v) {
                                if (v > 100) {
                                    System.out.println("capping");
                                    v = 100;
                                }
                                return v;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesIfWhereAssignedValueDiffersFromBoundaryAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Weird {
                            public int clamp(int v) {
                                if (v > 100) {
                                    v = 50;
                                }
                                return v;
                            }
                        }
                        """
                )
        );
    }
}
