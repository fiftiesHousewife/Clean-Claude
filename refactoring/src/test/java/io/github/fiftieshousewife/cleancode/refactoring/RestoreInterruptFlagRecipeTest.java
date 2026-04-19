package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RestoreInterruptFlagRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new RestoreInterruptFlagRecipe());
    }

    @Test
    void insertsInterruptCallWhenMissing() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Sleeper {
                            public void pause(long ms) {
                                try {
                                    Thread.sleep(ms);
                                } catch (InterruptedException e) {
                                    System.err.println("interrupted: " + e.getMessage());
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Sleeper {
                            public void pause(long ms) {
                                try {
                                    Thread.sleep(ms);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    System.err.println("interrupted: " + e.getMessage());
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesCatchThatAlreadyReinterrupts() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Already {
                            public void pause(long ms) {
                                try {
                                    Thread.sleep(ms);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesNonInterruptedExceptionCatchAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.IOException;
                        public class Other {
                            public void read() {
                                try {
                                    doRead();
                                } catch (IOException e) {
                                    System.err.println("io");
                                }
                            }
                            void doRead() throws IOException {}
                        }
                        """
                )
        );
    }

    @Test
    void leavesCatchThatRethrowsInterruptedException() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Rethrow {
                            public void pause() throws InterruptedException {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    throw e;
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void insertsInterruptEvenWhenCatchBodyIsEmpty() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Silent {
                            public void pause(long ms) {
                                try {
                                    Thread.sleep(ms);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Silent {
                            public void pause(long ms) {
                                try {
                                    Thread.sleep(ms);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                        """
                )
        );
    }
}
