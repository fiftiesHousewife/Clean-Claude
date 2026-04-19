package io.github.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessRecipePassTest {

    @TempDir
    Path tempDir;

    @Test
    void appliesMakeStaticAndDeleteSectionCommentsToTheSameFile() throws IOException {
        final Path file = tempDir.resolve("Mixed.java");
        Files.writeString(file, """
                package com.example;
                public class Mixed {
                    public int run() {
                        // Phase 1: start
                        int x = 1;
                        return x + 1;
                    }
                }
                """);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(List.of(file));
        final String after = Files.readString(file);

        assertAll(
                () -> assertEquals(1, summary.filesChanged(), "one file changed: " + summary),
                () -> assertTrue(summary.recipeNamesByFile().get(file)
                                .contains("MakeMethodStaticRecipe"),
                        "static was added: " + summary),
                () -> assertTrue(summary.recipeNamesByFile().get(file)
                                .contains("DeleteSectionCommentsRecipe"),
                        "section comment removed: " + summary),
                () -> assertTrue(after.contains("public static int run()"),
                        "file updated with static: " + after),
                () -> assertFalse(after.contains("Phase 1"),
                        "section comment stripped: " + after));
    }

    @Test
    void leavesFileUntouchedWhenNoRecipeFires() throws IOException {
        final Path file = tempDir.resolve("Clean.java");
        final String before = """
                package com.example;
                public class Clean {
                    private final int count = 0;

                    public int next() {
                        return count + 1;
                    }
                }
                """;
        Files.writeString(file, before);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(List.of(file));

        assertAll(
                () -> assertEquals(0, summary.filesChanged()),
                () -> assertEquals(before, Files.readString(file),
                        "unchanged file is byte-identical"));
    }

    @Test
    void returnsCombinedSummaryAcrossMultipleFiles() throws IOException {
        final Path fileA = tempDir.resolve("A.java");
        Files.writeString(fileA, """
                package com.example;
                public class A {
                    // Phase 1: do it
                    public int f() { return 1; }
                }
                """);
        final Path fileB = tempDir.resolve("B.java");
        Files.writeString(fileB, """
                package com.example;
                public class B {
                    public int g(int x) { return x; }
                }
                """);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(List.of(fileA, fileB));

        assertAll(
                () -> assertEquals(2, summary.filesChanged()),
                () -> assertTrue(summary.allRecipeNames()
                                .contains("MakeMethodStaticRecipe"),
                        "both files get static: " + summary));
    }
}
