package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
            Integer startLine,
            Integer endLine,
            String message,
            String severity,
            String confidence,
            String source,
            String tool,
            String ruleRef,
            Map<String, String> metadata
    ) {}

    public static void write(final AggregatedReport report, final Path outputFile) throws IOException {
        write(report, outputFile, List.of());
    }

    public static void write(final AggregatedReport report, final Path outputFile,
                             final List<SourceState> sourceStates) throws IOException {
        Files.createDirectories(outputFile.getParent());

        final Map<String, String> sourceDisplayNames = new HashMap<>();
        sourceStates.forEach(s -> sourceDisplayNames.put(s.id(), s.displayName()));

        final List<JsonFinding> jsonFindings = report.findings().stream()
                .map(f -> new JsonFinding(
                        f.code().name(),
                        f.sourceFile(),
                        nullableLine(f.sourceFile(), f.startLine()),
                        nullableLine(f.sourceFile(), f.endLine()),
                        f.message(),
                        f.severity().name(),
                        f.confidence().name(),
                        sourceDisplayNames.getOrDefault(f.tool(), f.tool()),
                        f.tool(),
                        f.ruleRef(),
                        f.metadata()))
                .toList();

        final JsonReport jsonReport = new JsonReport(
                report.projectName(),
                report.projectVersion(),
                report.generatedAt().toString(),
                jsonFindings);

        // serializeNulls keeps null sourceFile + null startLine/endLine
        // visible in the JSON so consumers can rely on field presence
        // and use jq's // fallback rather than re-deriving "absent" from
        // -1 sentinels.
        final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        Files.writeString(outputFile, gson.toJson(jsonReport));
    }

    /**
     * Project-global findings (e.g. T1 coverage) have no line anchor.
     * Emitting -1 in JSON forced consumers to special-case a magic
     * number; emitting null lets {@code jq '.startLine // "n/a"'} fall
     * through cleanly.
     */
    private static Integer nullableLine(final String sourceFile, final int line) {
        if (sourceFile == null || line < 0) {
            return null;
        }
        return line;
    }
}
