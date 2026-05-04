package io.github.fiftieshousewife.cleancode.plugin.serve;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefactoringApplierTest {

    @Test
    void appliesG34RecipeToSingleFileAndRewritesSource(@TempDir final Path projectRoot) throws IOException {
        final Path file = projectRoot.resolve("src/main/java/Foo.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                package x;
                public class Foo {
                    void run() {
                        // Phase 1: validate
                        int a = 1;
                        // Phase 2: compute
                        int b = a + 1;
                        System.out.println(b);
                    }
                }
                """);

        final RefactoringApplier applier = new RefactoringApplier(projectRoot);
        final RefactoringApplier.Outcome outcome = applier.apply(HeuristicCode.G34, Optional.of(file));

        final String after = Files.readString(file);
        assertAll(
                () -> assertTrue(outcome.success(),
                        "successful apply must report no errors; got " + outcome.errors()),
                () -> assertEquals(1, outcome.filesChanged()),
                () -> assertTrue(outcome.recipesFired().contains("DeleteSectionCommentsRecipe")),
                () -> assertFalse(after.contains("Phase 1"),
                        "section-marker comments must be deleted"));
    }

    @Test
    void reportsSoftErrorWhenRecipeMatchesNothing(@TempDir final Path projectRoot) throws IOException {
        final Path file = projectRoot.resolve("Foo.java");
        Files.writeString(file, """
                package x;
                public class Foo {
                    void run() {
                        int a = 1;
                        System.out.println(a);
                    }
                }
                """);

        final RefactoringApplier applier = new RefactoringApplier(projectRoot);
        final RefactoringApplier.Outcome outcome = applier.apply(HeuristicCode.G34, Optional.of(file));

        assertAll(
                () -> assertEquals(0, outcome.filesChanged()),
                () -> assertFalse(outcome.success(),
                        "no-op apply should surface as a soft error so the UI can show it"),
                () -> assertTrue(outcome.errors().get(0).contains("did not match anything"),
                        "error message points at the no-match cause: " + outcome.errors()));
    }

    @Test
    void reportsErrorWhenCodeHasNoRegisteredRecipe(@TempDir final Path projectRoot) {
        final RefactoringApplier applier = new RefactoringApplier(projectRoot);

        final RefactoringApplier.Outcome outcome = applier.apply(HeuristicCode.T1, Optional.empty());

        assertAll(
                () -> assertFalse(outcome.success()),
                () -> assertEquals(0, outcome.filesChanged()),
                () -> assertTrue(outcome.errors().get(0).contains("no fix recipe"),
                        "T1 has no fix recipe; error must say so: " + outcome.errors()));
    }

    @Test
    void bulkModeWalksWholeProjectExceptBuildAndGradle(@TempDir final Path projectRoot) throws IOException {
        final Path src = projectRoot.resolve("src/main/java/A.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, """
                package x;
                public class A {
                    void run() {
                        // Phase 1: setup
                        int a = 1;
                        System.out.println(a);
                    }
                }
                """);
        final Path buildArtifact = projectRoot.resolve("build/generated/B.java");
        Files.createDirectories(buildArtifact.getParent());
        Files.writeString(buildArtifact, """
                package x;
                public class B {
                    void run() {
                        // Phase 1: should NOT be touched
                        int a = 1;
                    }
                }
                """);

        final RefactoringApplier applier = new RefactoringApplier(projectRoot);
        final RefactoringApplier.Outcome outcome = applier.apply(HeuristicCode.G34, Optional.empty());

        assertAll(
                () -> assertTrue(outcome.success(),
                        "bulk apply succeeded: " + outcome.errors()),
                () -> assertFalse(Files.readString(src).contains("Phase 1"),
                        "src/main file got cleaned"),
                () -> assertTrue(Files.readString(buildArtifact).contains("Phase 1"),
                        "build/ artifacts must be skipped to avoid rewriting generated code"));
    }
}
