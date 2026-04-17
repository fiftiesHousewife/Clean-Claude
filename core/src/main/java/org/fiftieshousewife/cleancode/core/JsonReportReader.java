package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
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
        final JsonReport jsonReport = parse(inputFile);
        final List<Finding> findings = toFindings(jsonReport.findings());
        final Set<HeuristicCode> coveredCodes = coveredCodesOf(findings);
        final Instant generatedAt = Instant.parse(jsonReport.generatedAt());
        return new AggregatedReport(
                findings, coveredCodes, generatedAt,
                jsonReport.projectName(), jsonReport.projectVersion());
    }

    private static JsonReport parse(final Path inputFile) throws IOException {
        final String json = Files.readString(inputFile);
        return new Gson().fromJson(json, JsonReport.class);
    }

    private static List<Finding> toFindings(final List<JsonFinding> rawFindings) {
        final List<Finding> findings = new ArrayList<>(rawFindings.size());
        rawFindings.forEach(rf -> findings.add(toFinding(rf)));
        return findings;
    }

    private static Finding toFinding(final JsonFinding rf) {
        final Map<String, String> metadata = rf.metadata() != null ? rf.metadata() : Map.of();
        return new Finding(
                HeuristicCode.valueOf(rf.code()),
                rf.sourceFile(),
                rf.startLine(),
                rf.endLine(),
                rf.message(),
                Severity.valueOf(rf.severity()),
                Confidence.valueOf(rf.confidence()),
                rf.tool(),
                rf.ruleRef(),
                metadata);
    }

    private static Set<HeuristicCode> coveredCodesOf(final List<Finding> findings) {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        findings.forEach(f -> codes.add(f.code()));
        return codes;
    }
}
