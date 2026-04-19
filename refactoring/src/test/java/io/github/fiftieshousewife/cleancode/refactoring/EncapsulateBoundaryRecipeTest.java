package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncapsulateBoundaryRecipeTest {

    @Test
    void extractsLengthMinusOne() {
        final String result = runRecipe("""
                package com.example;
                public class Foo {
                    void method(String[] items) {
                        for (int i = 0; i < items.length - 1; i++) {
                            System.out.println(items[i]);
                        }
                    }
                }
                """);

        assertAll(
                () -> assertTrue(result.contains("lastIndex"), "Should have lastIndex variable"),
                () -> assertTrue(result.contains("final int lastIndex"), "Should declare lastIndex as final int")
        );
    }

    @Test
    void extractsSizeMinusOne() {
        final String result = runRecipe("""
                package com.example;
                import java.util.List;
                public class Foo {
                    void method(List<String> items) {
                        for (int i = 0; i < items.size() - 1; i++) {
                            System.out.println(items.get(i));
                        }
                    }
                }
                """);

        assertTrue(result.contains("lastIndex"), "Should have lastIndex variable");
    }

    @Test
    void leavesSimpleConditionAlone() {
        final var recipe = new EncapsulateBoundaryRecipe();
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse("""
                package com.example;
                public class Foo {
                    void method(String[] items) {
                        for (int i = 0; i < items.length; i++) {
                            System.out.println(items[i]);
                        }
                    }
                }
                """).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty());
    }

    private String runRecipe(String source) {
        final var recipe = new EncapsulateBoundaryRecipe();
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
