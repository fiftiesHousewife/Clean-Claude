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

public final class HtmlReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneOffset.UTC);

    private static final String STYLES = loadResource("/html-report/styles.css");
    private static final String DOCUMENT_TEMPLATE = loadResource("/html-report/document.html");
    private static final String SEVERITY_SUMMARY_TEMPLATE = loadResource("/html-report/severity-summary.html");
    private static final String CLEAN_MESSAGE = loadResource("/html-report/clean-message.html");

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
        final String body = renderBody(report, repositoryUrl);
        return DOCUMENT_TEMPLATE
                .replace("{{title}}", HtmlEscaping.escape(report.projectName()))
                .replace("{{styles}}", STYLES)
                .replace("{{projectLine}}", projectLine(report))
                .replace("{{body}}", body)
                .replace("{{timestamp}}", HtmlEscaping.escape(TIMESTAMP_FORMAT.format(report.generatedAt())))
                .replace("{{totalFindings}}", Integer.toString(report.findings().size()));
    }

    static String renderBody(final AggregatedReport report, final String repositoryUrl) {
        final StringBuilder body = new StringBuilder();
        body.append(renderSeveritySummary(report));
        if (report.findings().isEmpty()) {
            body.append(CLEAN_MESSAGE);
        } else {
            HtmlFindingsSection.appendByCode(body, report.findings(), repositoryUrl);
            HtmlFindingsSection.appendToolSummary(body, report.findings());
        }
        return body.toString();
    }

    static String renderSeveritySummary(final AggregatedReport report) {
        final Map<Severity, List<Finding>> bySeverity = report.bySeverity();
        final int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        final int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        final int info = bySeverity.getOrDefault(Severity.INFO, List.of()).size();
        return SEVERITY_SUMMARY_TEMPLATE
                .replace("{{errors}}", Integer.toString(errors))
                .replace("{{warnings}}", Integer.toString(warnings))
                .replace("{{info}}", Integer.toString(info));
    }

    static String projectLine(final AggregatedReport report) {
        final String escapedName = HtmlEscaping.escape(report.projectName());
        if (report.projectVersion() == null) {
            return escapedName;
        }
        return escapedName + " v" + HtmlEscaping.escape(report.projectVersion());
    }

    private static String loadResource(final String path) {
        try (InputStream in = HtmlReportWriter.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
