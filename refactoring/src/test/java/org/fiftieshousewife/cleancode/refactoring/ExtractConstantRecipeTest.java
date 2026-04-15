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

class ExtractConstantRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExtractConstantRecipe(2));
    }

    @Test
    void addsConstantForDuplicatedString() {
        final String source = """
                package com.example;
                public class Foo {
                    void method1() {
                        System.out.println("hello world");
                    }
                    void method2() {
                        System.out.println("hello world");
                    }
                }
                """;

        final var recipe = new ExtractConstantRecipe(2);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        final var changed = results.getChangeset().getAllResults();

        assertFalse(changed.isEmpty(), "Recipe should have made changes");
        final String result = changed.getFirst().getAfter().printAll();

        assertAll(
                () -> assertTrue(result.contains("private static final String HELLO_WORLD"),
                        "Should add constant field"),
                () -> assertTrue(result.contains("\"hello world\""),
                        "Should keep original string (replacement is a separate step)")
        );
    }

    @Test
    void leavesSingleOccurrenceAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method() {
                                System.out.println("unique string");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void ignoresShortStrings() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void method1() {
                                String a = "";
                                String b = "";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void ignoresStringsAlreadyInConstants() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private static final String MSG = "hello world";
                            void method() {
                                System.out.println(MSG);
                                System.out.println("hello world");
                            }
                        }
                        """
                )
        );
    }
}
