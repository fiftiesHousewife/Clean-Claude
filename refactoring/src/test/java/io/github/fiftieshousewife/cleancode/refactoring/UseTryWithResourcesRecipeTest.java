package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseTryWithResourcesRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new UseTryWithResourcesRecipe());
    }

    @Test
    void rewritesDeclarationBeforeTryAndCloseInFinally() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class Reader {
                            public byte[] read(Path path) throws Exception {
                                InputStream in = Files.newInputStream(path);
                                try {
                                    return in.readAllBytes();
                                } finally {
                                    in.close();
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class Reader {
                            public byte[] read(Path path) throws Exception {
                                try (InputStream in = Files.newInputStream(path)) {
                                    return in.readAllBytes();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void rewritesWhenFinallyUsesNullGuardedClose() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class Guarded {
                            public byte[] read(Path path) throws Exception {
                                InputStream in = Files.newInputStream(path);
                                try {
                                    return in.readAllBytes();
                                } finally {
                                    if (in != null) {
                                        in.close();
                                    }
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class Guarded {
                            public byte[] read(Path path) throws Exception {
                                try (InputStream in = Files.newInputStream(path)) {
                                    return in.readAllBytes();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void preservesCatchClausesWhileRewritingFinally() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.IOException;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class WithCatch {
                            public byte[] read(Path path) {
                                InputStream in;
                                try {
                                    in = Files.newInputStream(path);
                                } catch (IOException e) {
                                    return new byte[0];
                                }
                                try {
                                    return in.readAllBytes();
                                } catch (IOException e) {
                                    return new byte[0];
                                } finally {
                                    try { in.close(); } catch (IOException ignored) {}
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesTryWithoutFinallyAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class NoFinally {
                            public void run() {
                                String s = "hi";
                                try {
                                    System.out.println(s);
                                } catch (RuntimeException e) {
                                    // ignore
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesFinallyDoingMoreThanClose() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class BusyFinally {
                            public byte[] read(Path path) throws Exception {
                                InputStream in = Files.newInputStream(path);
                                try {
                                    return in.readAllBytes();
                                } finally {
                                    System.out.println("done");
                                    in.close();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesFinallyThatClosesDifferentVariable() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.InputStream;
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        public class MisaligningClose {
                            public byte[] read(Path path, InputStream other) throws Exception {
                                InputStream in = Files.newInputStream(path);
                                try {
                                    return in.readAllBytes();
                                } finally {
                                    other.close();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesDeclarationWithoutInitializer() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.io.InputStream;
                        public class NoInit {
                            public void read(InputStream src) throws Exception {
                                InputStream in;
                                in = src;
                                try {
                                    in.readAllBytes();
                                } finally {
                                    in.close();
                                }
                            }
                        }
                        """
                )
        );
    }
}
