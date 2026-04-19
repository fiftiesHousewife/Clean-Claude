package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RemoveNestedTernaryRecipeTest {

    @Test
    void replacesNestedTernaryWithIfElse() {
        final String result = runRecipe("""
                package com.example;
                public class Foo {
                    String classify(int x) {
                        return x > 0 ? "positive" : x < 0 ? "negative" : "zero";
                    }
                }
                """);

        assertAll(
                () -> assertTrue(result.contains("if"), "Should use if statement"),
                () -> assertTrue(result.contains("result"), "Should use result variable"),
                () -> assertFalse(result.contains("x > 0 ? \"positive\" : x < 0 ? \"negative\""),
                        "Should not have nested ternary")
        );
    }

    @Test
    void leavesSimpleTernaryAlone() {
        final var recipe = new RemoveNestedTernaryRecipe();
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse("""
                package com.example;
                public class Foo {
                    String classify(int x) {
                        return x > 0 ? "positive" : "non-positive";
                    }
                }
                """).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty());
    }

    private String runRecipe(String source) {
        final var recipe = new RemoveNestedTernaryRecipe();
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        final var changed = results.getChangeset().getAllResults();
        assertFalse(changed.isEmpty(), "Recipe should have made changes");
        return changed.getFirst().getAfter().printAll();
    }
}
