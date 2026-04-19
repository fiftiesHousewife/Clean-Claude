package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddLocaleRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddLocaleRecipe());
    }

    @Test
    void addsLocaleRootToToLowerCase() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            String lower(String s) {
                                return s.toLowerCase();
                            }
                        }
                        """,
                        """
                        package com.example;

                        import java.util.Locale;

                        public class Foo {
                            String lower(String s) {
                                return s.toLowerCase(Locale.ROOT);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesAlreadyParameterisedCallAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.Locale;
                        public class Foo {
                            String lower(String s) {
                                return s.toLowerCase(Locale.ENGLISH);
                            }
                        }
                        """
                )
        );
    }
}
