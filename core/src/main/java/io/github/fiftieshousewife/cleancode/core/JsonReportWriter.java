package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
    ) {}

    public static void write(AggregatedReport report, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());

        List<JsonFinding> jsonFindings = report.findings().stream()
                .map(f -> new JsonFinding(
                        f.code().name(),
                        f.sourceFile(),
                        f.startLine(),
                        f.endLine(),
                        f.message(),
                        f.severity().name(),
                        f.confidence().name(),
                        f.tool(),
                        f.ruleRef(),
                        f.metadata()))
                .toList();

        JsonReport jsonReport = new JsonReport(
                report.projectName(),
                report.projectVersion(),
                report.generatedAt().toString(),
                jsonFindings);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(outputFile, gson.toJson(jsonReport));
    }
}
