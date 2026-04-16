package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WrapAssertAllRecipeTest {

    @Test
    void wrapsConsecutiveAssertsInAssertAll() {
        final String result = runRecipe("""
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                class FooTest {
                    @Test
                    void checkStuff() {
                        String x = "hello";
                        assertEquals("hello", x);
                        assertTrue(x.length() > 0);
                        assertNotNull(x);
                    }
                }
                """);

        assertAll(
                () -> assertTrue(result.contains("assertAll"), "Should wrap in assertAll"),
                () -> assertTrue(result.contains("() ->"), "Should use lambdas")
        );
    }

    @Test
    void leavesAlreadyWrappedAssertsAlone() {
        final var recipe = new WrapAssertAllRecipe(2);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse("""
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                class FooTest {
                    @Test
                    void checkStuff() {
                        assertAll(
                            () -> assertEquals(1, 1),
                            () -> assertTrue(true)
                        );
                    }
                }
                """).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void leavesSingleAssertAlone() {
        final var recipe = new WrapAssertAllRecipe(2);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse("""
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                class FooTest {
                    @Test
                    void checkStuff() {
                        assertEquals(1, 1);
                    }
                }
                """).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty());
    }

    private String runRecipe(String source) {
        final var recipe = new WrapAssertAllRecipe(2);
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
