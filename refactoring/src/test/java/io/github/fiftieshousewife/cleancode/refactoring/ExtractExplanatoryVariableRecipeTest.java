package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtractExplanatoryVariableRecipeTest {

    @Test
    void extractsChainedMethodCallToVariable() {
        final String source = """
                package com.example;
                public class Foo {
                    void method(String input) {
                        if (input.trim().toLowerCase().startsWith("prefix")) {
                            System.out.println("match");
                        }
                    }
                }
                """;

        final String result = runRecipe(source);

        assertTrue(result.contains("final") || result.contains("var"),
                "Should extract to a named variable");
    }

    @Test
    void leavesSimpleConditionAlone() {
        final String source = """
                package com.example;
                public class Foo {
                    void method(String input) {
                        if (input != null) {
                            System.out.println(input);
                        }
                    }
                }
                """;

        final var recipe = new ExtractExplanatoryVariableRecipe(3);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty(),
                "Should not change simple conditions");
    }

    private String runRecipe(String source) {
        final var recipe = new ExtractExplanatoryVariableRecipe(3);
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
