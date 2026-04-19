package io.github.fiftieshousewife.cleancode.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CpdIntegrationTest {

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.fiftieshousewife.cleancode")
                }
                repositories {
                    mavenCentral()
                }
                cleanCode {
                    thresholds {
                        cpdMinimumTokens.set(30)
                    }
                }
                """);

        final Path srcDir = projectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        final String duplicatedBlock = """
                package com.example;
                public class %s {
                    public int calculate(int a, int b, int c) {
                        int result = a + b;
                        result = result * c;
                        result = result + (a * b);
                        result = result - (b * c);
                        if (result > 100) {
                            result = result / 2;
                        }
                        return result;
                    }
                }
                """;

        Files.writeString(srcDir.resolve("Foo.java"), duplicatedBlock.formatted("Foo"));
        Files.writeString(srcDir.resolve("Bar.java"), duplicatedBlock.formatted("Bar"));
    }

    @Test
    void cpdDetectsDuplicateCode() {
        final BuildResult result = runner("cpdMain").build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void analyseCleanCodeProducesReports() throws IOException {
        final BuildResult result = runner("analyseCleanCode").build();

        assertTrue(result.getOutput().contains("findings"), "Console should show finding count");

        final Path jsonReport = projectDir.resolve("build/reports/clean-code/findings.json");
        final Path htmlReport = projectDir.resolve("build/reports/clean-code/findings.html");
        final Path cpdReport = projectDir.resolve("build/reports/cpd/cpd.xml");

        assertAll(
                () -> assertTrue(Files.exists(jsonReport), "JSON report should exist"),
                () -> assertTrue(Files.exists(htmlReport), "HTML report should exist"),
                () -> assertTrue(Files.exists(cpdReport), "CPD XML report should exist"),
                () -> assertTrue(Files.readString(cpdReport).contains("duplication"),
                        "CPD report should contain duplication elements"),
                () -> assertTrue(Files.readString(jsonReport).contains("\"G5\""),
                        "JSON report should contain G5 duplication findings")
        );
    }

    private GradleRunner runner(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput();
    }
}
