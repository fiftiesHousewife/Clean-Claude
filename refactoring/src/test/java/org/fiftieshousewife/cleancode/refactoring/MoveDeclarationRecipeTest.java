package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveDeclarationRecipeTest {

    @Test
    void movesDeclarationCloserToFirstUse() {
        final String source = """
                package com.example;
                public class Foo {
                    void method() {
                        String name = "hello";
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                        int g = 7;
                        int h = 8;
                        int i = 9;
                        int j = 10;
                        System.out.println(name);
                    }
                }
                """;

        final String result = runRecipe(source);
        final int nameDecl = result.indexOf("String name");
        final int nameUse = result.indexOf("println(name)");
        assertTrue(nameDecl > 0 && nameUse > nameDecl,
                "name declaration should appear before its use");
    }

    @Test
    void leavesAlreadyCloseDeclaration() {
        final String source = """
                package com.example;
                public class Foo {
                    void method() {
                        String name = "hello";
                        System.out.println(name);
                    }
                }
                """;

        final var recipe = new MoveDeclarationRecipe(5);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);

        assertTrue(results.getChangeset().getAllResults().isEmpty(),
                "Should not change already-close declarations");
    }

    private String runRecipe(String source) {
        final var recipe = new MoveDeclarationRecipe(5);
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
