package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AutoFormatRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat(null));
    }

    @Test
    void fixesInconsistentIndentation() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                        void method() {
                        int x = 1;
                        }
                        }
                        """,
                        """
                        package com.example;

                        public class Foo {
                            void method() {
                                int x = 1;
                            }
                        }
                        """
                )
        );
    }
}
