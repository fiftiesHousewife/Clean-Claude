package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
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

    @Test
    void emitsVscodeUrlWithCopyIconWhenSchemeIsVscode(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");
        final Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);

        HtmlReportWriter.write(sampleReport(), output, "", projectRoot, "vscode");
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("vscode://file"),
                        "should contain vscode:// scheme"),
                () -> assertTrue(html.contains("class=\"copy-link\""),
                        "every clickable location should ship with a copy-to-clipboard fallback"),
                () -> assertTrue(html.contains("data-path="),
                        "links should carry data-path so the picker can rewrite them"),
                () -> assertTrue(html.contains("data-line="),
                        "links should carry data-line so the picker can rewrite them"));
    }

    @Test
    void emitsIdeaUrlWhenSchemeIsIdea(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");
        final Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);

        HtmlReportWriter.write(sampleReport(), output, "", projectRoot, "idea");
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("idea://open?file="),
                        "should contain idea:// scheme"),
                () -> assertTrue(html.contains("&line="),
                        "idea URL should encode line"));
    }

    @Test
    void emitsCursorUrlWhenSchemeIsCursor(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");
        final Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);

        HtmlReportWriter.write(sampleReport(), output, "", projectRoot, "cursor");
        final String html = Files.readString(output);

        assertTrue(html.contains("cursor://file"));
    }

    @Test
    void columnWidthsLeaveRoomForLongPathsAndUseFullViewport(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("max-width: min(1800px, 96vw)"),
                        "main width should expand on wider screens"),
                () -> assertTrue(html.contains("col.location { width: 38%"),
                        "location column should be wide enough for module + path + line"),
                () -> assertTrue(html.contains("col.severity { width: 5rem"),
                        "severity column should be tight"));
    }

    @Test
    void listsAllConfiguredSourcesIncludingMissingOnes(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");
        final Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);

        final List<SourceState> sourceStates = List.of(
                SourceState.produced("openrewrite", "OpenRewrite", 2),
                SourceState.ranNoFindings("checkstyle", "Checkstyle"),
                SourceState.notAvailable("jacoco", "JaCoCo"));

        HtmlReportWriter.write(sampleReport(), output, "", projectRoot, "vscode", sourceStates);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("OpenRewrite"),
                        "tool that produced findings should appear"),
                () -> assertTrue(html.contains("ran, no findings"),
                        "tools that ran but produced nothing should be visible to users"),
                () -> assertTrue(html.contains("not available")
                        && html.contains("JaCoCo"),
                        "tools whose report file was missing should be flagged"));
    }

    @Test
    void includesOptionalRulesPanelByDefault(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("Optional rules")),
                () -> assertTrue(html.contains("checkstyle:FinalLocalVariable")),
                () -> assertTrue(html.contains("enabledOptionalRules"),
                        "panel should tell users how to opt in"));
    }

    @Test
    void emitsIdePickerWhenFindingsPresent(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");
        final Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);

        HtmlReportWriter.write(sampleReport(), output, "", projectRoot, "vscode");
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("id=\"ide-scheme\""),
                        "should include the IDE picker dropdown"),
                () -> assertTrue(html.contains("buildIdeUrl"),
                        "should include the rewiring script"),
                () -> assertTrue(html.contains("buildCliCommand"),
                        "should include the CLI fallback builder"),
                () -> assertTrue(html.contains("localStorage"),
                        "picker selection should persist across visits"));
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
