package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

class RenameShortNameRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RenameShortNameRecipe(Map.of("x", "index", "n", "count")));
    }

    @Test
    void renamesFieldAndLocalVariableByMap() {
        final String source = """
                package com.example;
                public class Foo {
                    int x = 0;
                    int bump() {
                        int n = 3;
                        return x + n;
                    }
                }
                """;

        final var recipe = new RenameShortNameRecipe(Map.of("x", "index", "n", "count"));
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var result = recipe.run(new InMemoryLargeSourceSet(parsed),
                new InMemoryExecutionContext(Throwable::printStackTrace));
        final var changed = result.getChangeset().getAllResults();

        assertFalse(changed.isEmpty(), "both variables should have been renamed");
        final String after = changed.getFirst().getAfter().printAll();
        assertAll(
                () -> assertTrue(after.contains("int index"), "field should use the new name"),
                () -> assertTrue(after.contains("int count"), "local should use the new name"),
                () -> assertTrue(after.contains("return index + count"),
                        "usages should be rewritten too"));
    }

    @Test
    void leavesForLoopIndexAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            int sum(int[] values) {
                                int total = 0;
                                for (int x = 0; x < values.length; x++) {
                                    total += values[x];
                                }
                                return total;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForEachIndexAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Foo {
                            int sum(List<Integer> values) {
                                int total = 0;
                                for (Integer x : values) {
                                    total += x;
                                }
                                return total;
                            }
                        }
                        """
                )
        );
    }
}
