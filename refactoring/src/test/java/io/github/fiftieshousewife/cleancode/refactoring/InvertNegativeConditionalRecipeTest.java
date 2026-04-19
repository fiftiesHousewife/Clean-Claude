package io.github.fiftieshousewife.cleancode.refactoring;

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

class InvertNegativeConditionalRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InvertNegativeConditionalRecipe());
    }

    @Test
    void invertsNotConditionWithElseBranch() {
        final String source = """
                package com.example;
                public class Foo {
                    String describe(boolean ready) {
                        if (!ready) {
                            return "waiting";
                        } else {
                            return "go";
                        }
                    }
                }
                """;

        final var recipe = new InvertNegativeConditionalRecipe();
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var result = recipe.run(new InMemoryLargeSourceSet(parsed),
                new InMemoryExecutionContext(Throwable::printStackTrace));
        final var changed = result.getChangeset().getAllResults();
        assertFalse(changed.isEmpty(), "should invert the condition");
        final String after = changed.getFirst().getAfter().printAll();

        final int positiveBranch = after.indexOf("if (ready)");
        final int waitingBranch = after.indexOf("waiting");
        final int goBranch = after.indexOf("go");
        assertAll(
                () -> assertTrue(positiveBranch >= 0, "condition should be positive (ready)"),
                () -> assertTrue(goBranch < waitingBranch,
                        "positive branch body comes before the negative one"));
    }

    @Test
    void leavesPositiveConditionAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            String describe(boolean ready) {
                                if (ready) {
                                    return "go";
                                } else {
                                    return "waiting";
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesElseIfChainsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            String describe(boolean a, boolean b) {
                                if (!a) {
                                    return "first";
                                } else if (b) {
                                    return "second";
                                } else {
                                    return "third";
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesGuardClauseAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            String describe(boolean ready) {
                                if (!ready) {
                                    return "waiting";
                                }
                                return "go";
                            }
                        }
                        """
                )
        );
    }
}
