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

class ExtractClassConstantRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExtractClassConstantRecipe(2));
    }

    @Test
    void addsConstantForDuplicatedIntegerLiteral() {
        final String source = """
                package com.example;
                public class Foo {
                    int a() { return 1000 + 1; }
                    int b() { return 1000 - 1; }
                }
                """;

        final var recipe = new ExtractClassConstantRecipe(2);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        final var changed = results.getChangeset().getAllResults();

        assertFalse(changed.isEmpty(), "recipe should make a change");
        final String result = changed.getFirst().getAfter().printAll();
        assertAll(
                () -> assertTrue(result.contains("private static final int CONSTANT_1000 = 1000"),
                        "should add an int constant field"),
                () -> assertTrue(result.contains("1000 + 1"),
                        "call sites are left untouched — renaming is a follow-up step"));
    }

    @Test
    void skipsTrivialZeroOneMinusOne() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            int a() { return 0 + 1; }
                            int b() { return 1 - 0; }
                            int c() { return -1; }
                            int d() { return -1; }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotRedeclareIfConstantAlreadyPresent() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private static final int THRESHOLD = 1000;
                            int a() { return 1000 + 1; }
                            int b() { return 1000 - 1; }
                        }
                        """
                )
        );
    }
}
