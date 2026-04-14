package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportWriterTest {

    @Test
    void writesValidHtmlFile(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);

        final String html = Files.readString(output);
        assertAll(
                () -> assertTrue(Files.exists(output)),
                () -> assertTrue(html.startsWith("<!DOCTYPE html>")),
                () -> assertTrue(html.contains("</html>"))
        );
    }

    @Test
    void containsFindingDetails(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);

        final String html = Files.readString(output);
        assertAll(
                () -> assertTrue(html.contains("Foo.java")),
                () -> assertTrue(html.contains("duplicated block")),
                () -> assertTrue(html.contains("Low coverage")),
                () -> assertTrue(html.contains("G5")),
                () -> assertTrue(html.contains("T1"))
        );
    }

    @Test
    void containsSeveritySummary(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);

        final String html = Files.readString(output);
        assertAll(
                () -> assertTrue(html.contains("1 errors")),
                () -> assertTrue(html.contains("1 warnings")),
                () -> assertTrue(html.contains("0 info"))
        );
    }

    @Test
    void containsBookReferences(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);

        final String html = Files.readString(output);
        assertAll(
                () -> assertTrue(html.contains("Clean Code Ch.17")),
                () -> assertTrue(html.contains("Duplication")),
                () -> assertTrue(html.contains("Insufficient Tests"))
        );
    }

    private AggregatedReport sampleReport() {
        final Finding f1 = new Finding(HeuristicCode.G5, "Foo.java", 10, 20,
                "duplicated block", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-dup",
                Map.of("otherFile", "Bar.java", "tokens", "150"));
        final Finding f2 = Finding.projectLevel(HeuristicCode.T1, "Low coverage",
                Severity.ERROR, Confidence.HIGH, "jacoco", "coverage");

        return new AggregatedReport(
                List.of(f1, f2),
                Set.of(HeuristicCode.G5, HeuristicCode.T1),
                Instant.parse("2026-01-15T10:30:00Z"),
                "test-project", "1.0");
    }
}
