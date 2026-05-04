package io.github.fiftieshousewife.cleancode.plugin.serve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeApplierTest {

    @Test
    void appliesAllThreeChangeKindsInOneBatch(@TempDir final Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve("build.gradle.kts"), """
                cleanCode {
                    disabledRecipes = listOf("G36")
                }
                """);
        final Path source = projectRoot.resolve("Foo.java");
        Files.writeString(source, """
                package com.example;
                public class Foo {
                    public void bar() {}
                }
                """);

        final ApplyChangesResponse response = new ChangeApplier(projectRoot).apply(List.of(
                new PendingChange("disableRecipe", Map.of("code", "G30"), "noisy"),
                new PendingChange("tuneThreshold",
                        Map.of("code", "T1", "threshold", "privateMethodMinLines", "newValue", "20"),
                        "we have a lot of small private helpers"),
                new PendingChange("suppressFinding",
                        Map.of("code", "G24", "file", "Foo.java", "line", "3"),
                        "intentional one-liner getter")));

        assertAll(
                () -> assertTrue(response.success(), "errors=" + response.errors()),
                () -> assertEquals(3, response.applied()),
                () -> assertTrue(Files.readString(projectRoot.resolve("build.gradle.kts"))
                        .contains("\"G30\"")),
                () -> assertTrue(Files.readString(projectRoot.resolve("build.gradle.kts"))
                        .contains("privateMethodMinLines = 20")),
                () -> assertTrue(Files.readString(source).contains("@SuppressWarnings(\"CleanCode:G24\")")),
                () -> assertTrue(Files.readString(source).contains(
                        "// CleanCode-suppress G24: intentional one-liner getter")));
    }

    @Test
    void groupsMultipleSuppressionsAgainstSameFileIntoOneWrite(@TempDir final Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve("build.gradle.kts"), "cleanCode {}\n");
        final Path source = projectRoot.resolve("Foo.java");
        Files.writeString(source, """
                package com.example;
                public class Foo {
                    public void a() {}
                    public void b() {}
                }
                """);

        final ApplyChangesResponse response = new ChangeApplier(projectRoot).apply(List.of(
                new PendingChange("suppressFinding",
                        Map.of("code", "G24", "file", "Foo.java", "line", "3"), "reason a"),
                new PendingChange("suppressFinding",
                        Map.of("code", "G30", "file", "Foo.java", "line", "4"), "reason b")));

        assertAll(
                () -> assertTrue(response.success(), "errors=" + response.errors()),
                () -> assertEquals(2, response.applied()));

        final String content = Files.readString(source);
        assertAll(
                () -> assertTrue(content.contains("@SuppressWarnings(\"CleanCode:G24\")")),
                () -> assertTrue(content.contains("@SuppressWarnings(\"CleanCode:G30\")")));
    }

    @Test
    void surfacesErrorsAndReportsPartialSuccess(@TempDir final Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve("build.gradle.kts"), "cleanCode {}\n");
        final Path source = projectRoot.resolve("Foo.java");
        Files.writeString(source, """
                package com.example;
                public class Foo {
                    public void bar() {}
                }
                """);

        final ApplyChangesResponse response = new ChangeApplier(projectRoot).apply(List.of(
                new PendingChange("disableRecipe", Map.of("code", "G30"), "ok change"),
                new PendingChange("suppressFinding",
                        Map.of("code", "G24", "file", "Foo.java", "line", "999"),
                        "out of range")));

        assertAll(
                () -> assertFalse(response.success(), "any failure flips success to false"),
                () -> assertEquals(1, response.applied(),
                        "the disableRecipe still landed even though the suppress failed"),
                () -> assertTrue(response.errors().stream()
                        .anyMatch(e -> e.contains("no enclosing declaration")),
                        "out-of-range line surfaces the parser-side message"));
    }

    @Test
    void refusesApplyRefactoringWhenWorkingTreeIsDirty(@TempDir final Path projectRoot) throws Exception {
        // Initialise a real git repo with one tracked file, then leave the
        // file modified — `git status --porcelain` will surface the change
        // and ChangeApplier should refuse to run any Fix click in this state.
        Files.writeString(projectRoot.resolve("build.gradle.kts"), "cleanCode {}\n");
        Files.writeString(projectRoot.resolve("Foo.java"), "package x;\nclass Foo {}\n");
        runGit(projectRoot, "init", "-q");
        runGit(projectRoot, "config", "user.email", "test@example.com");
        runGit(projectRoot, "config", "user.name", "Test");
        runGit(projectRoot, "add", ".");
        runGit(projectRoot, "commit", "-q", "-m", "init");
        Files.writeString(projectRoot.resolve("Foo.java"),
                "package x;\nclass Foo { void modified(){} }\n");

        final ApplyChangesResponse response = new ChangeApplier(projectRoot).apply(List.of(
                new PendingChange("applyRefactoring",
                        Map.of("code", "G29", "file", "Foo.java"),
                        "applying invert-negative recipe")));

        assertAll(
                () -> assertFalse(response.success(),
                        "Fix must refuse to run on a dirty tree"),
                () -> assertEquals(0, response.applied()),
                () -> assertTrue(response.errors().stream()
                        .anyMatch(e -> e.contains("Working tree has uncommitted changes")),
                        "error message should explain the safety rule"));
    }

    @Test
    void allowsSuppressionsOnDirtyTreeBecauseTheyAreSmallAndReviewable(@TempDir final Path projectRoot)
            throws Exception {
        Files.writeString(projectRoot.resolve("build.gradle.kts"), "cleanCode {}\n");
        Files.writeString(projectRoot.resolve("Foo.java"), """
                package x;
                public class Foo {
                    public void bar() {}
                }
                """);
        runGit(projectRoot, "init", "-q");
        runGit(projectRoot, "config", "user.email", "test@example.com");
        runGit(projectRoot, "config", "user.name", "Test");
        runGit(projectRoot, "add", ".");
        runGit(projectRoot, "commit", "-q", "-m", "init");
        Files.writeString(projectRoot.resolve("Foo.java"), """
                package x;
                public class Foo {
                    public void bar() { System.out.println("dirty"); }
                }
                """);

        final ApplyChangesResponse response = new ChangeApplier(projectRoot).apply(List.of(
                new PendingChange("disableRecipe", Map.of("code", "G30"), "noisy")));

        assertTrue(response.success(),
                "non-applyRefactoring batches must NOT be blocked by a dirty tree: " + response.errors());
    }

    private static void runGit(final Path repo, final String... args) throws IOException, InterruptedException {
        final java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("git");
        java.util.Collections.addAll(cmd, args);
        final Process p = new ProcessBuilder(cmd).directory(repo.toFile()).redirectErrorStream(true).start();
        final String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed: " + out);
        }
    }
}
