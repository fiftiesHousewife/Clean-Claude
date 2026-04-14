package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeMdGeneratorTest {

    @Test
    void generatesFindingSectionsGroupedByCode(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G5, "Bar.java", 5, 15, "dup2", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G4, "Baz.java", 1, 1, "empty", Severity.ERROR, Confidence.HIGH, "pmd", "r")
        );

        ClaudeMdGenerator.generate(report, output, null);

        String content = Files.readString(output);
        assertTrue(content.contains("G4"));
        assertTrue(content.contains("G5"));
        assertTrue(content.contains("2 findings"));
    }

    @Test
    void findingSectionStartsWithSkillPointer(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.Ch7_1, "Foo.java", 10, 10, "catch", Severity.WARNING, Confidence.MEDIUM, "openrewrite", "r")
        );

        ClaudeMdGenerator.generate(report, output, null);

        String content = Files.readString(output);
        assertTrue(content.contains("> Read `.claude/skills/ch7-exception-handling.md`"));
    }

    @Test
    void generatedSectionsWrappedInMarkers(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "r")
        );

        ClaudeMdGenerator.generate(report, output, null);

        String content = Files.readString(output);
        assertTrue(content.contains("<!-- GENERATED: G5 -->"));
        assertTrue(content.contains("<!-- /GENERATED -->"));
    }

    @Test
    void annotateSectionsPreservedOnRegeneration(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "r")
        );

        ClaudeMdGenerator.generate(report, output, null);

        // Inject an ANNOTATE section
        String original = Files.readString(output);
        String withAnnotation = original.replace("<!-- /GENERATED -->",
                "<!-- /GENERATED -->\n\n<!-- ANNOTATE: G5 -->\nTeam note: ignore Foo.java\n<!-- /ANNOTATE -->");
        Files.writeString(output, withAnnotation);

        // Regenerate
        ClaudeMdGenerator.generate(report, output, null);

        String regenerated = Files.readString(output);
        assertTrue(regenerated.contains("Team note: ignore Foo.java"));
    }

    @Test
    void preambleAlwaysPresent(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        AggregatedReport report = reportWith();

        ClaudeMdGenerator.generate(report, output, null);

        String content = Files.readString(output);
        assertTrue(content.contains("Before you start any work"));
    }

    @Test
    void deltaTableGeneratedWhenBaselineExists(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("CLAUDE.md");
        Path baseline = tempDir.resolve("baseline.json");

        // Write a baseline with G5: 5
        Files.writeString(baseline, """
                {"counts":{"G5":5}}
                """);

        AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G5, "Foo.java", 10, 20, "dup", Severity.WARNING, Confidence.HIGH, "cpd", "r"),
                Finding.at(HeuristicCode.G5, "Bar.java", 5, 15, "dup2", Severity.WARNING, Confidence.HIGH, "cpd", "r")
        );

        ClaudeMdGenerator.generate(report, output, baseline);

        String content = Files.readString(output);
        assertTrue(content.contains("Baseline"));
        assertTrue(content.contains("Delta"));
    }

    private AggregatedReport reportWith(Finding... findings) {
        Set<HeuristicCode> covered = Set.of();
        return new AggregatedReport(List.of(findings), covered, Instant.now(), "test-project", "1.0");
    }
}
