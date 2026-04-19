package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.recipes.NegativeConditionalRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NegativeConditionalRecipeTest {

    @Test
    void detectsNegatedIsNotMethod() {
        final var rows = runRecipe("""
                package com.example;
                public class Foo {
                    boolean isNotEmpty() { return true; }
                    void check() {
                        if (!isNotEmpty()) {}
                    }
                }
                """);

        assertEquals(1, rows.size());
    }

    @Test
    void detectsNegatedHasNoMethod() {
        final var rows = runRecipe("""
                package com.example;
                public class Foo {
                    boolean hasNoAccess() { return true; }
                    void check() {
                        if (!hasNoAccess()) {}
                    }
                }
                """);

        assertEquals(1, rows.size());
    }

    @Test
    void ignoresSimpleNegation() {
        final var rows = runRecipe("""
                package com.example;
                public class Foo {
                    boolean isEmpty() { return true; }
                    void check() {
                        if (!isEmpty()) {}
                    }
                }
                """);

        assertTrue(rows.isEmpty());
    }

    private List<NegativeConditionalRecipe.Row> runRecipe(String source) {
        final var recipe = new NegativeConditionalRecipe();
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        return recipe.collectedRows();
    }
}
