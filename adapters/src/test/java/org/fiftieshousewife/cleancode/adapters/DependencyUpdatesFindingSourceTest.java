package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyUpdatesFindingSourceTest {

    private DependencyUpdatesFindingSource source;

    @BeforeEach
    void setUp() {
        source = new DependencyUpdatesFindingSource();
    }

    @Test
    void returnsE1FindingsForOutdatedDependencies(@TempDir Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.openrewrite",
                        "name": "rewrite-core",
                        "version": "8.40.2",
                        "available": { "milestone": "8.79.3" }
                      },
                      {
                        "group": "com.google.code.gson",
                        "name": "gson",
                        "version": "2.10.1",
                        "available": { "release": "2.11.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertAll(
                () -> assertEquals(2, findings.size()),
                () -> assertEquals(HeuristicCode.E1, findings.get(0).code()),
                () -> assertTrue(findings.get(0).message().contains("rewrite-core")),
                () -> assertTrue(findings.get(0).message().contains("8.40.2")),
                () -> assertTrue(findings.get(0).message().contains("8.79.3")),
                () -> assertTrue(findings.get(1).message().contains("gson"))
        );
    }

    @Test
    void returnsEmptyWhenNoReportExists(@TempDir Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoOutdatedDependencies(@TempDir Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": { "dependencies": [] },
                  "current": { "dependencies": [] }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.isEmpty());
    }

    @Test
    void isNotAvailableWhenReportMissing(@TempDir Path tempDir) {
        final Path buildDir = tempDir.resolve("build");
        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        assertFalse(source.isAvailable(context));
    }

    @Test
    void isAvailableWhenReportExists(@TempDir Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                { "outdated": { "dependencies": [] } }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        assertTrue(source.isAvailable(context));
    }

    @Test
    void prefersMilestoneOverReleaseVersion(@TempDir Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "milestone": "1.1.0", "release": "1.2.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.get(0).message().contains("1.1.0"));
    }

    private void writeReport(Path buildDir, String json) throws Exception {
        final Path reportFile = buildDir.resolve("dependencyUpdates/report.json");
        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, json);
    }

    private ProjectContext contextWithBuildDir(Path tempDir, Path buildDir) {
        return new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(),
                buildDir, buildDir.resolve("reports"), List.of());
    }
}
