package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

class RecordToLombokValueRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RecordToLombokValueRecipe(4));
    }

    @Test
    void convertsLargeRecordToClassWithAnnotations() {
        final String source = """
                package com.example;
                public record Config(
                        String name,
                        String host,
                        int port,
                        boolean ssl,
                        int timeout
                ) {}
                """;

        final String result = runAndGetSource(source);

        assertAll(
                () -> assertTrue(result.contains("class Config"), "Should be a class, not a record"),
                () -> assertFalse(result.contains("record Config"), "Should not be a record"),
                () -> assertTrue(result.contains("@Value"), "Should have @Value annotation"),
                () -> assertTrue(result.contains("@Builder"), "Should have @Builder annotation"),
                () -> assertTrue(result.contains("String name;"), "Should have name field"),
                () -> assertTrue(result.contains("int port;"), "Should have port field"),
                () -> assertTrue(result.contains("int timeout;"), "Should have timeout field")
        );
    }

    @Test
    void leavesSmallRecordAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public record Pair(String key, String value) {}
                        """
                )
        );
    }

    @Test
    void leavesClassAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Foo {
                            private String name;
                        }
                        """
                )
        );
    }

    @Test
    void preservesExistingImports() {
        final String source = """
                package com.example;
                import java.util.List;
                public record Config(
                        String name,
                        List<String> tags,
                        int port,
                        boolean ssl,
                        int timeout
                ) {}
                """;

        final String result = runAndGetSource(source);

        assertAll(
                () -> assertTrue(result.contains("import java.util.List"), "Should keep existing import"),
                () -> assertTrue(result.contains("List<String> tags;"), "Should have tags field")
        );
    }

    private String runAndGetSource(String source) {
        final var recipe = new RecordToLombokValueRecipe(4);
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
