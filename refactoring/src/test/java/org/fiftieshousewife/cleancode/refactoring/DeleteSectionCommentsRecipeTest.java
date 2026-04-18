package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DeleteSectionCommentsRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new DeleteSectionCommentsRecipe());
    }

    @Test
    void deletesNumberedPhaseCommentBeforeStatementSequence() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public void run() {
                                // Phase 1: validate inputs.
                                int x = 1;
                                int y = 2;
                                // Phase 2: compute result.
                                int z = x + y;
                                System.out.println(z);
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Foo {
                            public void run() {
                                int x = 1;
                                int y = 2;
                                int z = x + y;
                                System.out.println(z);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void deletesStepPrefixedComments() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Steps {
                            public void go() {
                                // Step 1: begin.
                                start();
                                // Step 2: end.
                                stop();
                            }
                            void start() {}
                            void stop() {}
                        }
                        """,
                        """
                        package com.example;
                        public class Steps {
                            public void go() {
                                start();
                                stop();
                            }
                            void start() {}
                            void stop() {}
                        }
                        """
                )
        );
    }

    @Test
    void leavesNonSectionCommentsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Keeper {
                            public int multiply(int a, int b) {
                                // Negative inputs would invert the sign — callers handle that upstream.
                                return a * b;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesJavadocAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Doc {
                            /**
                             * Phase 1: load the config. Actually real Javadoc that describes
                             * the loading sequence for callers.
                             */
                            public void load() {
                            }
                        }
                        """
                )
        );
    }

    @Test
    void deletesSectionCommentWithoutTrailingPunctuation() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Bare {
                            public void go() {
                                // Phase 1
                                doWork();
                            }
                            void doWork() {}
                        }
                        """,
                        """
                        package com.example;
                        public class Bare {
                            public void go() {
                                doWork();
                            }
                            void doWork() {}
                        }
                        """
                )
        );
    }

    @Test
    void leavesBlankLineOnlyCommentAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Blank {
                            public void go() {
                                // this is a regular line comment
                                doWork();
                            }
                            void doWork() {}
                        }
                        """
                )
        );
    }
}
