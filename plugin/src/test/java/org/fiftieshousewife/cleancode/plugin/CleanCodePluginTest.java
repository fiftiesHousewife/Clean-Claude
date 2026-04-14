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

    private GradleRunner runner(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath();
    }
}
