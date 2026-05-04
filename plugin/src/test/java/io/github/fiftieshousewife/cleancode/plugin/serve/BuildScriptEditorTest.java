package io.github.fiftieshousewife.cleancode.plugin.serve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildScriptEditorTest {

    @Test
    void appendsCodeToExistingDisabledRecipesList(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                plugins { id("java") }
                cleanCode {
                    disabledRecipes = listOf("G36")
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.disableRecipe("G30");
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("disabledRecipes = listOf(\"G36\", \"G30\")"),
                        "appended to existing list with comma-space separator"),
                () -> assertTrue(result.contains("plugins { id(\"java\") }"),
                        "leaves the rest of the script untouched"));
    }

    @Test
    void createsDisabledRecipesLineWhenAbsent(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                    failOnViolation.set(false)
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.disableRecipe("G30");
        editor.save();

        final String result = Files.readString(script);
        assertTrue(result.contains("disabledRecipes = listOf(\"G30\")"),
                "inserts a fresh disabledRecipes line inside the cleanCode block");
    }

    @Test
    void createsCleanCodeBlockWhenAbsent(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, "plugins { id(\"java\") }\n");

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.disableRecipe("G30");
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("cleanCode {")),
                () -> assertTrue(result.contains("disabledRecipes = listOf(\"G30\")")));
    }

    @Test
    void disableIsIdempotent(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                    disabledRecipes = listOf("G30")
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.disableRecipe("G30");
        editor.save();

        final String result = Files.readString(script);
        final long count = result.lines().filter(l -> l.contains("\"G30\"")).count();
        assertEquals(1, count, "duplicate code must not be appended");
    }

    @Test
    void replacesExistingThresholdValue(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                    thresholds {
                        methodBlankLineSections = 6
                        privateMethodMinLines = 12
                    }
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.tuneThreshold("methodBlankLineSections", 10);
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("methodBlankLineSections = 10"),
                        "value updated in place"),
                () -> assertTrue(result.contains("privateMethodMinLines = 12"),
                        "siblings preserved"));
    }

    @Test
    void insertsNewThresholdKeyInsideExistingBlock(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                    thresholds {
                        methodBlankLineSections = 6
                    }
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.tuneThreshold("classLineCount", 200);
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("methodBlankLineSections = 6")),
                () -> assertTrue(result.contains("classLineCount = 200")));
    }

    @Test
    void createsThresholdsBlockWhenAbsent(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                    failOnViolation.set(false)
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.tuneThreshold("methodBlankLineSections", 8);
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("thresholds {")),
                () -> assertTrue(result.contains("methodBlankLineSections = 8")),
                () -> assertTrue(result.contains("failOnViolation.set(false)"),
                        "existing config preserved"));
    }

    @Test
    void multipleEditsAccumulateBeforeSave(@TempDir final Path tempDir) throws IOException {
        final Path script = tempDir.resolve("build.gradle.kts");
        Files.writeString(script, """
                cleanCode {
                }
                """);

        final BuildScriptEditor editor = new BuildScriptEditor(script);
        editor.disableRecipe("G30");
        editor.disableRecipe("T1");
        editor.tuneThreshold("methodBlankLineSections", 8);
        editor.save();

        final String result = Files.readString(script);
        assertAll(
                () -> assertTrue(result.contains("\"G30\"")),
                () -> assertTrue(result.contains("\"T1\"")),
                () -> assertTrue(result.contains("methodBlankLineSections = 8")));
    }
}
