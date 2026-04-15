package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DeleteUnusedImportRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeleteUnusedImportRecipe());
    }

    @Test
    void removesUnusedImport() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        import java.util.Map;
                        public class Foo {
                            List<String> items;
                        }
                        """,
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

    @Test
    void keepsUsedImports() {
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

    @Test
    void expandsStarImport() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.*;
                        public class Foo {
                            List<String> items;
                            Map<String, String> map;
                        }
                        """,
                        """
                        package com.example;
                        import java.util.List;
                        import java.util.Map;
                        public class Foo {
                            List<String> items;
                            Map<String, String> map;
                        }
                        """
                )
        );
    }
}
