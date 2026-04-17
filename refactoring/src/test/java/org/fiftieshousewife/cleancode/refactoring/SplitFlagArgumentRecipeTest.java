package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

class SplitFlagArgumentRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SplitFlagArgumentRecipe());
    }

    @Test
    void emitsTwoHelpersForPrivateMethodWithBooleanFlag() {
        final String source = """
                package com.example;
                public class Foo {
                    private void log(boolean verbose) {
                        if (verbose) {
                            System.out.println("noisy");
                        } else {
                            System.out.println("quiet");
                        }
                    }
                }
                """;

        final var recipe = new SplitFlagArgumentRecipe();
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var result = recipe.run(new InMemoryLargeSourceSet(parsed),
                new InMemoryExecutionContext(Throwable::printStackTrace));
        final var changed = result.getChangeset().getAllResults();

        assertFalse(changed.isEmpty(), "recipe should emit helper methods");
        final String after = changed.getFirst().getAfter().printAll();
        assertAll(
                () -> assertTrue(after.contains("logWhenVerbose"),
                        "true-branch helper should be named <method>When<Flag>"),
                () -> assertTrue(after.contains("logWhenVerboseIsFalse"),
                        "false-branch helper should be named <method>When<Flag>IsFalse"),
                () -> assertTrue(after.contains("private void log(boolean verbose)"),
                        "original method is kept for the agent to delete once call sites are migrated"));
    }

    @Test
    void ignoresPublicMethod() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            public void log(boolean verbose) {
                                if (verbose) {
                                    System.out.println("x");
                                } else {
                                    System.out.println("y");
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void ignoresMethodWhereBodyIsNotSingleIfElse() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private void log(boolean verbose) {
                                System.out.println("header");
                                if (verbose) {
                                    System.out.println("x");
                                } else {
                                    System.out.println("y");
                                }
                            }
                        }
                        """
                )
        );
    }
}
