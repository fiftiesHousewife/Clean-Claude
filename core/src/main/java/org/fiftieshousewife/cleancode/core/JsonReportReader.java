package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonReportReader {

    private JsonReportReader() {}

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

    public static AggregatedReport read(final Path inputFile) throws IOException {
        final JsonReport jsonReport = parseJson(inputFile);
        final List<Finding> findings = jsonReport.findings().stream()
                .map(JsonReportReader::toFinding)
                .toList();
        return new AggregatedReport(
                findings,
                coveredCodes(findings),
                Instant.parse(jsonReport.generatedAt()),
                jsonReport.projectName(),
                jsonReport.projectVersion());
    }

    private static JsonReport parseJson(final Path inputFile) throws IOException {
        final String json = Files.readString(inputFile);
        return new Gson().fromJson(json, JsonReport.class);
    }

    private static Finding toFinding(final JsonFinding raw) {
        return new Finding(
                HeuristicCode.valueOf(raw.code()),
                raw.sourceFile(),
                raw.startLine(),
                raw.endLine(),
                raw.message(),
                Severity.valueOf(raw.severity()),
                Confidence.valueOf(raw.confidence()),
                raw.tool(),
                raw.ruleRef(),
                raw.metadata() != null ? raw.metadata() : Map.of());
    }

    private static Set<HeuristicCode> coveredCodes(final List<Finding> findings) {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        findings.forEach(finding -> codes.add(finding.code()));
        return codes;
    }
}
