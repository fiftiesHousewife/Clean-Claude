package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.fiftieshousewife.cleancode.core.HtmlEscaping.escape;
import static org.fiftieshousewife.cleancode.core.HtmlFindingsRenderer.appendFindingsByCode;
import static org.fiftieshousewife.cleancode.core.HtmlFindingsRenderer.appendToolSummary;

public final class HtmlReportWriter {

    private static final String TEMPLATE_RESOURCE = "/org/fiftieshousewife/cleancode/core/html-report.html";
    private static final String TEMPLATE = loadTemplate();

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneOffset.UTC);

    private static final String NO_FINDINGS_BODY =
            "    <p class=\"clean\">No violations found. The code is clean.</p>\n";

    private HtmlReportWriter() {}

    public static void write(final AggregatedReport report, final Path outputFile) throws IOException {
        write(report, outputFile, "");
    }

    public static void write(final AggregatedReport report, final Path outputFile,
                              final String repositoryUrl) throws IOException {
        final Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, render(report, repositoryUrl));
    }

    static String render(final AggregatedReport report, final String repositoryUrl) {
        return TEMPLATE
                .replace("{projectName}", escape(report.projectName()))
                .replace("{styles}", HtmlReportStyles.css())
                .replace("{projectHeading}", projectHeading(report))
                .replace("{errorCount}", severityCount(report, Severity.ERROR))
                .replace("{warningCount}", severityCount(report, Severity.WARNING))
                .replace("{infoCount}", severityCount(report, Severity.INFO))
                .replace("{body}", body(report, repositoryUrl))
                .replace("{timestamp}", escape(TIMESTAMP_FORMAT.format(report.generatedAt())))
                .replace("{totalFindings}", Integer.toString(report.findings().size()));
    }

    static String projectHeading(final AggregatedReport report) {
        final String name = escape(report.projectName());
        if (report.projectVersion() == null) {
            return name;
        }
        return name + " v" + escape(report.projectVersion());
    }

    static String severityCount(final AggregatedReport report, final Severity severity) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        return Integer.toString(bySeverity.getOrDefault(severity, List.of()).size());
    }

    static String body(final AggregatedReport report, final String repositoryUrl) {
        if (report.findings().isEmpty()) {
            return NO_FINDINGS_BODY;
        }
        final StringBuilder html = new StringBuilder();
        appendFindingsByCode(html, report.findings(), repositoryUrl);
        appendToolSummary(html, report.findings());
        return html.toString();
    }

    private static String loadTemplate() {
        try (InputStream stream = HtmlReportWriter.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing HTML template resource: " + TEMPLATE_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to load HTML template: " + TEMPLATE_RESOURCE, exception);
        }
    }
}
