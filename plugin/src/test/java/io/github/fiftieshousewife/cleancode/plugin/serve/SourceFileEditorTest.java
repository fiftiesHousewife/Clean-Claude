package io.github.fiftieshousewife.cleancode.plugin.serve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceFileEditorTest {

    @Test
    void insertsAnnotationAndCommentAboveTargetMethod(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;

                public class Foo {

                    public void bar() {
                        System.out.println("hi");
                    }
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        final SourceFileEditor.Result result = editor.suppressFinding(5, "G30",
                "intentionally split into readable sections");
        editor.save();

        final String content = Files.readString(file);
        assertAll(
                () -> assertTrue(result.success(), "result.error=" + result.error()),
                () -> assertTrue(content.contains("// CleanCode-suppress G30: intentionally split into readable sections")),
                () -> assertTrue(content.contains("@SuppressWarnings(\"CleanCode:G30\")")),
                () -> assertTrue(content.contains("public void bar()"),
                        "method declaration is preserved exactly"));
    }

    @Test
    void targetsClassDeclarationWhenLineIsAtClassLevel(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;

                public class Foo {
                    private int x;
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        editor.suppressFinding(3, "G8", "x is intentionally package-visible for testing");
        editor.save();

        final String content = Files.readString(file);
        final int classIdx = content.indexOf("public class Foo");
        final int annIdx = content.indexOf("@SuppressWarnings(\"CleanCode:G8\")");
        assertAll(
                () -> assertTrue(annIdx > 0 && annIdx < classIdx,
                        "annotation should be on the line above 'public class Foo'"),
                () -> assertTrue(content.contains("// CleanCode-suppress G8:")));
    }

    @Test
    void mergesIntoExistingSuppressWarningsWithSingleLiteral(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("unchecked")
                    public void bar() {}
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        editor.suppressFinding(4, "G30", "another reason");
        editor.save();

        final String content = Files.readString(file);
        assertAll(
                () -> assertTrue(content.contains("@SuppressWarnings({\"unchecked\", \"CleanCode:G30\"})"),
                        "single-string form is upgraded to array form including the new code"),
                () -> assertEquals(1,
                        content.lines().filter(l -> l.contains("@SuppressWarnings")).count(),
                        "merged into the existing annotation, not duplicated"));
    }

    @Test
    void mergesIntoExistingSuppressWarningsWithArray(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    public void bar() {}
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        editor.suppressFinding(4, "G30", "needs work");
        editor.save();

        final String content = Files.readString(file);
        assertTrue(content.contains("@SuppressWarnings({\"unchecked\", \"rawtypes\", \"CleanCode:G30\"})"),
                "array form gets the new code appended");
    }

    @Test
    void idempotentWhenSameCodeAlreadySuppressed(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    @SuppressWarnings("CleanCode:G30")
                    public void bar() {}
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        editor.suppressFinding(4, "G30", "still relevant");
        editor.save();

        final String content = Files.readString(file);
        assertEquals(1, content.lines().filter(l -> l.contains("@SuppressWarnings")).count(),
                "no duplicate annotation when the code is already suppressed");
    }

    @Test
    void chooseSmallestEnclosingDeclarationForNestedMethod(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    public void outer() {
                        Runnable r = new Runnable() {
                            @Override public void run() {
                                System.out.println("x");
                            }
                        };
                    }
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        editor.suppressFinding(6, "G30", "noisy line");
        editor.save();

        final String content = Files.readString(file);
        final int annIdx = content.indexOf("@SuppressWarnings(\"CleanCode:G30\")");
        final int runIdx = content.indexOf("public void run()");
        assertTrue(annIdx > 0 && annIdx < runIdx,
                "annotation attaches to the innermost run() method, not the outer class");
    }

    @Test
    void failsClearlyWhenLineHasNoEnclosingDeclaration(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, """
                package com.example;
                public class Foo {
                    public void bar() {}
                }
                """);

        final SourceFileEditor editor = new SourceFileEditor(file);
        final SourceFileEditor.Result result = editor.suppressFinding(99, "G30", "out of range");

        assertAll(
                () -> assertFalse(result.success()),
                () -> assertTrue(result.error().contains("no enclosing declaration"),
                        "error names the failure mode so the client can surface it"));
    }
}
