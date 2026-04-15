package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.ProjectContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestContexts {

    private TestContexts() {}

    static ProjectContext contextWithFixture(Class<?> testClass, Path tempDir,
                                             String resourcePath, String targetPath) throws IOException {
        return contextWithFixture(testClass, tempDir, resourcePath, targetPath, Path.of("/project"));
    }

    static ProjectContext contextWithFixture(Class<?> testClass, Path tempDir,
                                             String resourcePath, String targetPath,
                                             Path projectRoot) throws IOException {
        Path reportsDir = tempDir.resolve("reports");
        Path targetFile = reportsDir.resolve(targetPath);
        Files.createDirectories(targetFile.getParent());

        try (InputStream is = testClass.getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Test fixture not found: " + resourcePath);
            Files.copy(is, targetFile);
        }

        return new ProjectContext(
                projectRoot, "test-project", "1.0", "21",
                List.of(), List.of(Path.of("src/test/java")),
                tempDir.resolve("build"), reportsDir, List.of());
    }
}
