package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ShortenFullyQualifiedReferencesRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedReferencesRecipe());
    }

    @Test
    void replacesFullyQualifiedParameterTypeWithImport() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            void register(java.util.List<String> items) {}
                        }
                        """,
                        """
                        package com.example;

                        import java.util.List;

                        public class Foo {
                            void register(List<String> items) {}
                        }
                        """
                )
        );
    }

    @Test
    void leavesAlreadyImportedTypesAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            List<String> items;
                        }
                        """
                )
        );
    }
}
