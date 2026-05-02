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
    void emitsStagingBarAndModalForServeUx(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("id=\"staging-bar\""),
                        "top-bar holds the pending-changes counter and confirm/discard"),
                () -> assertTrue(html.contains("id=\"stage-modal\""),
                        "modal captures reason before staging"),
                () -> assertTrue(html.contains("cleanCodePendingChanges"),
                        "pending changes persist across reloads via localStorage"),
                () -> assertTrue(html.contains("/api/state"),
                        "client fetches current config to render correct toggle states"),
                () -> assertTrue(html.contains("/api/apply-changes"),
                        "confirm POSTs the batch to the server"));
    }

    @Test
    void emitsPerFindingSuppressButtonWithCodeFileAndLine(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("class=\"action-btn suppress-btn\""),
                        "every actionable finding gets a Suppress button"),
                () -> assertTrue(html.contains("data-code=\"G5\"")
                        && html.contains("data-file=\"Foo.java\"")
                        && html.contains("data-line=\"10\""),
                        "Suppress button carries the data the server needs to insert the annotation"));
    }

    @Test
    void emitsDisableAndTuneButtonsPerCodeSection(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        final Finding f = new Finding(HeuristicCode.G30, "Foo.java", 1, 1,
                "method has too many blank-line sections",
                Severity.WARNING, Confidence.HIGH, "openrewrite", "G30", Map.of());
        final AggregatedReport report = new AggregatedReport(
                List.of(f), Set.of(HeuristicCode.G30),
                java.time.Instant.now(), "test", "1.0");

        HtmlReportWriter.write(report, output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("class=\"action-btn disable-btn\""),
                        "every code section gets a Disable button"),
                () -> assertTrue(html.contains("class=\"action-btn tune-btn\"")
                        && html.contains("data-threshold=\"methodBlankLineSections\""),
                        "G30 has a tunable threshold so it gets the Tune button mapped to the right key"));
    }

    @Test
    void omitsTuneButtonForCodesWithoutThreshold(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        final Finding f = new Finding(HeuristicCode.G36, "Foo.java", 1, 1,
                "too much information",
                Severity.WARNING, Confidence.HIGH, "openrewrite", "G36", Map.of());
        final AggregatedReport report = new AggregatedReport(
                List.of(f), Set.of(HeuristicCode.G36),
                java.time.Instant.now(), "test", "1.0");

        HtmlReportWriter.write(report, output);
        final String html = Files.readString(output);

        assertTrue(!html.contains("class=\"action-btn tune-btn\""),
                "G36 has no configurable threshold — Tune button must not appear");
    }

    @Test
    void modalRequiresReasonBeforeStaging(@TempDir Path tempDir) throws Exception {
        final Path output = tempDir.resolve("report.html");

        HtmlReportWriter.write(sampleReport(), output);
        final String html = Files.readString(output);

        assertAll(
                () -> assertTrue(html.contains("required minlength=\"5\""),
                        "modal enforces a meaningful reason via HTML validation"),
                () -> assertTrue(html.contains("id=\"modal-reason\"")),
                () -> assertTrue(html.contains("Reason (required, min 5 chars)"),
                        "label tells the user reason is mandatory"));
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
