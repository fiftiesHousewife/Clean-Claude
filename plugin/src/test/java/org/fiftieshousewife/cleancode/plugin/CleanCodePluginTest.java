package org.fiftieshousewife.cleancode.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CleanCodePluginTest {

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.fiftieshousewife.cleancode")
                }
                """);
    }

    @Test
    void pluginAppliesWithoutError() {
        BuildResult result = runner("tasks").build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void analyseCleanCodeTaskRegistered() {
        BuildResult result = runner("tasks", "--all").build();
        assertTrue(result.getOutput().contains("analyseCleanCode"));
    }

    @Test
    void generateClaudeMdTaskRegistered() {
        BuildResult result = runner("tasks", "--all").build();
        assertTrue(result.getOutput().contains("generateClaudeMd"));
    }

    @Test
    void cleanCodeBaselineTaskRegistered() {
        BuildResult result = runner("tasks", "--all").build();
        assertTrue(result.getOutput().contains("cleanCodeBaseline"));
    }

    @Test
    void cleanCodeExplainTaskRegistered() {
        BuildResult result = runner("tasks", "--all").build();
        assertTrue(result.getOutput().contains("cleanCodeExplain"));
    }

    @Test
    void cpdTaskUsesDefaultMinimumTokens() {
        final BuildResult result = runner("cpdMain", "--dry-run").build();
        assertTrue(result.getOutput().contains("cpdMain"));
    }

    @Test
    void cpdTaskUsesCustomMinimumTokens() throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.fiftieshousewife.cleancode")
                }
                cleanCode {
                    thresholds {
                        cpdMinimumTokens.set(100)
                    }
                }
                """);
        final BuildResult result = runner("tasks").build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void scaffoldsClassesSkillFileWithDefaultThresholds() throws IOException {
        runner("tasks").build();
        final Path classesSkill = projectDir.resolve(".claude/skills/classes.md");
        assertTrue(Files.exists(classesSkill), "classes.md should be scaffolded");
        final String content = Files.readString(classesSkill);
        assertAll(
                () -> assertTrue(content.contains("150 lines"), "should contain default class line count"),
                () -> assertTrue(content.contains("more than 6 fields"), "should contain default record component count"),
                () -> assertTrue(content.contains("around 50 lines"), "should contain default class target lines"),
                () -> assertFalse(content.contains("{{"), "should not contain unresolved tokens")
        );
    }

    @Test
    void scaffoldsClassesSkillFileWithCustomThresholds() throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.fiftieshousewife.cleancode")
                }
                cleanCode {
                    thresholds {
                        classLineCount.set(200)
                        recordComponentCount.set(8)
                    }
                }
                """);
        runner("tasks").build();
        final Path classesSkill = projectDir.resolve(".claude/skills/classes.md");
        final String content = Files.readString(classesSkill);
        assertAll(
                () -> assertTrue(content.contains("200 lines"), "should contain custom class line count"),
                () -> assertTrue(content.contains("more than 8 fields"), "should contain custom record component count"),
                () -> assertFalse(content.contains("150 lines"), "should not contain default class line count"),
                () -> assertFalse(content.contains("{{"), "should not contain unresolved tokens")
        );
    }

    @Test
    void doesNotReScaffoldWhenThresholdsUnchanged() throws IOException {
        runner("tasks").build();
        final Path classesSkill = projectDir.resolve(".claude/skills/classes.md");
        final long firstModified = Files.getLastModifiedTime(classesSkill).toMillis();

        runner("tasks").build();
        final long secondModified = Files.getLastModifiedTime(classesSkill).toMillis();
        assertEquals(firstModified, secondModified, "file should not be re-written when thresholds unchanged");
    }

    @Test
    void reScaffoldsWhenThresholdsChange() throws IOException {
        runner("tasks").build();
        final Path classesSkill = projectDir.resolve(".claude/skills/classes.md");
        final String originalContent = Files.readString(classesSkill);
        assertTrue(originalContent.contains("150 lines"));

        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.fiftieshousewife.cleancode")
                }
                cleanCode {
                    thresholds {
                        classLineCount.set(200)
                    }
                }
                """);
        runner("tasks").build();
        final String updatedContent = Files.readString(classesSkill);
        assertAll(
                () -> assertTrue(updatedContent.contains("200 lines"), "should contain new threshold"),
                () -> assertFalse(updatedContent.contains("150 lines"), "should not contain old threshold")
        );
    }

    @Test
    void warnsWhenThresholdsChangeButFileCustomised() throws IOException {
        runner("tasks").build();
        final Path classesSkill = projectDir.resolve(".claude/skills/classes.md");
        Files.writeString(classesSkill, "# My custom skill file\nCustom content here.\n");

        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.fiftieshousewife.cleancode")
                }
                cleanCode {
                    thresholds {
                        classLineCount.set(200)
                    }
                }
                """);
        final BuildResult result = runner("tasks").build();
        final String customContent = Files.readString(classesSkill);
        assertAll(
                () -> assertTrue(customContent.contains("My custom skill file"), "customised file should not be overwritten"),
                () -> assertTrue(result.getOutput().contains("manual update needed"), "should warn about manual update")
        );
    }

    private GradleRunner runner(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath();
    }
}
