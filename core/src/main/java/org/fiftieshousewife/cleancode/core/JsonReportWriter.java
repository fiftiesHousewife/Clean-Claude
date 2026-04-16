package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.annotations.SuppressCleanCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class JsonReportWriter {

    private JsonReportWriter() {}

    private record JsonReport(
            String projectName,
            String projectVersion,
            String generatedAt,
            List<JsonFinding> findings
    ) {}

    @SuppressCleanCode(value = HeuristicCode.F1,
            reason = "JsonFinding is the JSON wire format and field names must remain flat for compatibility; "
                    + "the Builder below is the intended call-site for new code.")
    private record JsonFinding(
            String code,
            String sourceFile,
            int startLine,
            int endLine,
            String message,
            String severity,
            String confidence,
            String tool,
            String ruleRef,
            Map<String, String> metadata
    ) {
        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private String code;
            private String sourceFile;
            private int startLine;
            private int endLine;
            private String message;
            private String severity;
            private String confidence;
            private String tool;
            private String ruleRef;
            private Map<String, String> metadata;

            private Builder() {
            }

            Builder code(final String code) {
                this.code = code;
                return this;
            }

            Builder sourceFile(final String sourceFile) {
                this.sourceFile = sourceFile;
                return this;
            }

            Builder startLine(final int startLine) {
                this.startLine = startLine;
                return this;
            }

            Builder endLine(final int endLine) {
                this.endLine = endLine;
                return this;
            }

            Builder message(final String message) {
                this.message = message;
                return this;
            }

            Builder severity(final String severity) {
                this.severity = severity;
                return this;
            }

            Builder confidence(final String confidence) {
                this.confidence = confidence;
                return this;
            }

            Builder tool(final String tool) {
                this.tool = tool;
                return this;
            }

            Builder ruleRef(final String ruleRef) {
                this.ruleRef = ruleRef;
                return this;
            }

            Builder metadata(final Map<String, String> metadata) {
                this.metadata = metadata;
                return this;
            }

            JsonFinding build() {
                return new JsonFinding(code, sourceFile, startLine, endLine,
                        message, severity, confidence, tool, ruleRef, metadata);
            }
        }
    }

    public static void write(final AggregatedReport report, final Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());

        final List<JsonFinding> jsonFindings = report.findings().stream()
                .map(JsonReportWriter::toJsonFinding)
                .toList();

        final JsonReport jsonReport = new JsonReport(
                report.projectName(),
                report.projectVersion(),
                report.generatedAt().toString(),
                jsonFindings);

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(outputFile, gson.toJson(jsonReport));
    }

    private static JsonFinding toJsonFinding(final Finding finding) {
        return JsonFinding.builder()
                .code(finding.code().name())
                .sourceFile(finding.sourceFile())
                .startLine(finding.startLine())
                .endLine(finding.endLine())
                .message(finding.message())
                .severity(finding.severity().name())
                .confidence(finding.confidence().name())
                .tool(finding.tool())
                .ruleRef(finding.ruleRef())
                .metadata(finding.metadata())
                .build();
    }
}
