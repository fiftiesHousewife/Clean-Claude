package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
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
    void returnsE1FindingsForOutdatedDependencies(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.openrewrite",
                        "name": "rewrite-core",
                        "version": "8.40.2",
                        "available": { "release": "8.79.3" }
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
    void returnsEmptyWhenNoReportExists(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoOutdatedDependencies(@TempDir final Path tempDir) throws Exception {
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
    void isNotAvailableWhenReportMissing(@TempDir final Path tempDir) {
        final Path buildDir = tempDir.resolve("build");
        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        assertFalse(source.isAvailable(context));
    }

    @Test
    void isAvailableWhenReportExists(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                { "outdated": { "dependencies": [] } }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);

        assertTrue(source.isAvailable(context));
    }

    @Test
    void anchorsFindingsToVersionCatalogWhenPresent(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      }
                    ]
                  }
                }
                """);
        Files.createDirectories(tempDir.resolve("gradle"));
        Files.writeString(tempDir.resolve("gradle/libs.versions.toml"), """
                [libraries]
                lib = { module = "org.example:lib", version = "1.0.0" }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertEquals("gradle/libs.versions.toml", findings.get(0).sourceFile(),
                "when a catalog exists, route E1 to the catalog so all findings land in one brief");
    }

    @Test
    void leavesFindingsProjectLevelWhenNoCatalogExists(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertNull(findings.get(0).sourceFile(),
                "without a catalog, E1 stays project-level (no source anchor)");
    }

    @Test
    void skipsE1WhenCatalogLivesInAncestor(@TempDir final Path tempDir) throws Exception {
        final Path rootDir = tempDir.resolve("repo-root");
        final Path moduleDir = rootDir.resolve("module");
        Files.createDirectories(rootDir.resolve("gradle"));
        Files.writeString(rootDir.resolve("gradle/libs.versions.toml"), "[versions]\n");
        Files.writeString(rootDir.resolve("settings.gradle.kts"), "rootProject.name = \"r\"");

        final Path buildDir = moduleDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = new ProjectContext(
                moduleDir, "module", "1.0", "21",
                List.of(), List.of(),
                buildDir, buildDir.resolve("reports"), List.of());

        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.isEmpty(),
                "sub-modules must defer E1 emission to the project that owns the catalog");
    }

    @Test
    void emitsE3WhenLatestVersionIsMajorBump(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "framework",
                        "version": "10.21.4",
                        "available": { "release": "13.4.2" }
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
                () -> assertEquals(HeuristicCode.E3, findings.get(0).code(),
                        "framework 10 → 13 is a major-version bump"),
                () -> assertEquals(HeuristicCode.E1, findings.get(1).code(),
                        "Gson 2.10 → 2.11 stays minor → still E1"));
    }

    @Test
    void treatsExoticVersionStringsAsPatchMinorByDefault(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "RELEASE-2024",
                        "available": { "release": "RELEASE-2026" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertEquals(HeuristicCode.E1, findings.get(0).code(),
                "unparseable leading numeric component falls back to E1 to avoid surprise E3 noise");
    }

    @Test
    void recognisesVPrefixedVersionsForMajorComparison(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "v1.4.0",
                        "available": { "release": "v2.0.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertEquals(HeuristicCode.E3, findings.get(0).code());
    }

    @Test
    void deduplicatesCoordinatesAcrossReport(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      },
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertEquals(1, findings.size(),
                "same coordinate across multiple configurations must collapse to one finding");
    }

    @Test
    void recommendsReleaseEvenWhenMilestoneIsHigher(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "milestone": "2.0.0-alpha.1", "release": "1.2.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertAll(
                () -> assertEquals(1, findings.size()),
                () -> assertTrue(findings.get(0).message().contains("1.2.0")),
                () -> assertFalse(findings.get(0).message().contains("alpha"),
                        "must never recommend a milestone/RC/alpha version"));
    }

    @Test
    void skipsDependencyWhenOnlyMilestoneOrIntegrationAvailable(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "org.example",
                        "name": "lib",
                        "version": "1.0.0",
                        "available": { "milestone": "2.0.0-alpha.1", "integration": "2.0.0-SNAPSHOT" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertTrue(findings.isEmpty(),
                "without a stable release, no E1 finding — pre-release versions never count as 'outdated'");
    }

    @Test
    void filtersToCoordinatesDeclaredInVersionCatalog(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "com.google.code.gson",
                        "name": "gson",
                        "version": "2.10.1",
                        "available": { "release": "2.11.0" }
                      },
                      {
                        "group": "com.puppycrawl.tools",
                        "name": "checkstyle",
                        "version": "10.21.4",
                        "available": { "release": "13.4.2" }
                      }
                    ]
                  }
                }
                """);
        Files.createDirectories(tempDir.resolve("gradle"));
        Files.writeString(tempDir.resolve("gradle/libs.versions.toml"), """
                [versions]
                gson = "2.10.1"

                [libraries]
                gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertAll(
                () -> assertEquals(1, findings.size(),
                        "only catalog-declared coordinates produce E1 findings"),
                () -> assertTrue(findings.get(0).message().contains("gson")),
                () -> assertFalse(findings.get(0).message().contains("checkstyle"),
                        "Checkstyle is pulled in by the cleancode plugin, not declared in the consumer's catalog"));
    }

    @Test
    void readsCatalogShorthandLibraryDeclarations(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
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
        Files.createDirectories(tempDir.resolve("gradle"));
        Files.writeString(tempDir.resolve("gradle/libs.versions.toml"), """
                [libraries]
                gson = "com.google.code.gson:gson:2.10.1"
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertEquals(1, findings.size(),
                "shorthand 'group:name:version' library entries are recognised by the catalog filter");
    }

    @Test
    void skipsCleanCodeInternalCoordinatesWhenNoCatalogPresent(@TempDir final Path tempDir) throws Exception {
        final Path buildDir = tempDir.resolve("build");
        writeReport(buildDir, """
                {
                  "outdated": {
                    "dependencies": [
                      {
                        "group": "com.puppycrawl.tools",
                        "name": "checkstyle",
                        "version": "10.21.4",
                        "available": { "release": "13.4.2" }
                      },
                      {
                        "group": "net.sourceforge.pmd",
                        "name": "pmd-java",
                        "version": "7.9.0",
                        "available": { "release": "7.24.0" }
                      },
                      {
                        "group": "com.example",
                        "name": "user-lib",
                        "version": "1.0.0",
                        "available": { "release": "1.1.0" }
                      }
                    ]
                  }
                }
                """);

        final ProjectContext context = contextWithBuildDir(tempDir, buildDir);
        final List<Finding> findings = source.collectFindings(context);

        assertAll(
                () -> assertEquals(1, findings.size(),
                        "Checkstyle and PMD come from the cleancode plugin classpath; only the user's lib remains"),
                () -> assertTrue(findings.get(0).message().contains("user-lib")));
    }

    private void writeReport(final Path buildDir, final String json) throws Exception {
        final Path reportFile = buildDir.resolve("dependencyUpdates/report.json");
        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, json);
    }

    private ProjectContext contextWithBuildDir(final Path tempDir, final Path buildDir) {
        return new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(),
                buildDir, buildDir.resolve("reports"), List.of());
    }
}
