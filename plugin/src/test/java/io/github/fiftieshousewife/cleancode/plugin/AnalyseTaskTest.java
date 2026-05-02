package io.github.fiftieshousewife.cleancode.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyseTaskTest {

    @Test
    void detectsIdeaWhenIdeaFolderPresent(@TempDir Path projectRoot) throws IOException {
        Files.createDirectories(projectRoot.resolve(".idea"));

        assertEquals("idea", AnalyseTask.detectIdeUrlScheme(projectRoot));
    }

    @Test
    void detectsVscodeWhenVscodeFolderPresent(@TempDir Path projectRoot) throws IOException {
        Files.createDirectories(projectRoot.resolve(".vscode"));

        assertEquals("vscode", AnalyseTask.detectIdeUrlScheme(projectRoot));
    }

    @Test
    void detectsCursorWhenCursorFolderPresent(@TempDir Path projectRoot) throws IOException {
        Files.createDirectories(projectRoot.resolve(".cursor"));

        assertEquals("cursor", AnalyseTask.detectIdeUrlScheme(projectRoot));
    }

    @Test
    void defaultsToIdeaWhenNoIdeFolderPresent(@TempDir Path projectRoot) {
        assertEquals("idea", AnalyseTask.detectIdeUrlScheme(projectRoot),
                "IDEA is the default — JetBrains has the largest Java IDE share, "
                        + "and 'idea://' is registered by Toolbox without extra setup");
    }

    @Test
    void preferIdeaOverVscodeWhenBothPresent(@TempDir Path projectRoot) throws IOException {
        Files.createDirectories(projectRoot.resolve(".idea"));
        Files.createDirectories(projectRoot.resolve(".vscode"));

        assertEquals("idea", AnalyseTask.detectIdeUrlScheme(projectRoot),
                ".idea wins over .vscode when both are present (IDEA is primary IDE)");
    }
}
