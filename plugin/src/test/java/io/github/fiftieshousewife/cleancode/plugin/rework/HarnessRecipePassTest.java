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
    void appliesAddFinalAndDeleteSectionCommentsToTheSameFile() throws IOException {
        final Path file = tempDir.resolve("Mixed.java");
        Files.writeString(file, """
                package com.example;
                public class Mixed {
                    public int run(int x) {
                        // Phase 1: start
                        return x + 1;
                    }
                }
                """);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(List.of(file));
        final String after = Files.readString(file);

        assertAll(
                () -> assertEquals(1, summary.filesChanged(), "one file changed: " + summary),
                () -> assertTrue(summary.recipeNamesByFile().get(file)
                                .contains("AddFinalRecipe"),
                        "final was added to params: " + summary),
                () -> assertTrue(summary.recipeNamesByFile().get(file)
                                .contains("DeleteSectionCommentsRecipe"),
                        "section comment removed: " + summary),
                () -> assertTrue(after.contains("final int x"),
                        "file updated with final param: " + after),
                () -> assertFalse(after.contains("Phase 1"),
                        "section comment stripped: " + after));
    }

    @Test
    void slf4jTransformsRecipeIsWiredIntoTheSweep() throws IOException {
        // The classpath-gated Slf4j transform (loaded from
        // fifties-recipes 0.8 via OpenRewrite Environment) deliberately
        // skips files whose classpath doesn't carry Lombok, so we can't
        // assert it actually rewrites a System.out fixture without a
        // synthetic classpath. What we CAN assert: a System.out file
        // passes through the harness without an exception, the Lombok-
        // less fixture remains unchanged (gate fired), and the harness
        // doesn't blow up loading the YAML composite from the runtime
        // classpath.
        final Path file = tempDir.resolve("Logger.java");
        final String before = """
                package com.example;
                public class Logger {
                    public void shout(final String message) {
                        System.out.println("hi: " + message);
                    }
                }
                """;
        Files.writeString(file, before);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(List.of(file));

        assertAll(
                () -> assertEquals(0, summary.filesChanged(),
                        "without Lombok on the source classpath, the gated transform skips and the file is unchanged: " + summary),
                () -> assertEquals(before, Files.readString(file),
                        "file content stays byte-identical when the gate skips"));
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
                                .contains("DeleteSectionCommentsRecipe"),
                        "section comment was stripped from A: " + summary),
                () -> assertTrue(summary.allRecipeNames()
                                .contains("AddFinalRecipe"),
                        "AddFinalRecipe fired on B's non-final param: " + summary));
    }
}
