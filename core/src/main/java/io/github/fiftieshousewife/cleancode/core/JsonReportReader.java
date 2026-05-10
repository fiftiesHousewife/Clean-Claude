package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

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

    public static AggregatedReport read(final Path inputFile) throws IOException {
        String json = Files.readString(inputFile);
        Gson gson = new Gson();
        Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());

        String projectName = (String) raw.get("projectName");
        String projectVersion = (String) raw.get("projectVersion");
        Instant generatedAt = Instant.parse((String) raw.get("generatedAt"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawFindings = (List<Map<String, Object>>) raw.get("findings");

        List<Finding> findings = new ArrayList<>();
        Set<HeuristicCode> coveredCodes = EnumSet.noneOf(HeuristicCode.class);

        for (Map<String, Object> rf : rawFindings) {
            HeuristicCode code = HeuristicCode.valueOf((String) rf.get("code"));
            coveredCodes.add(code);

            String sourceFile = (String) rf.get("sourceFile");
            int startLine = readLine(rf.get("startLine"));
            int endLine = readLine(rf.get("endLine"));
            String message = (String) rf.get("message");
            Severity severity = Severity.valueOf((String) rf.get("severity"));
            Confidence confidence = Confidence.valueOf((String) rf.get("confidence"));
            String tool = (String) rf.get("tool");
            String ruleRef = (String) rf.get("ruleRef");

            @SuppressWarnings("unchecked")
            Map<String, String> metadata = rf.get("metadata") != null
                    ? (Map<String, String>) rf.get("metadata")
                    : Map.of();

            findings.add(new Finding(code, sourceFile, startLine, endLine,
                    message, severity, confidence, tool, ruleRef, metadata));
        }

        return new AggregatedReport(findings, coveredCodes, generatedAt, projectName, projectVersion);
    }

    /**
     * Project-global findings (e.g. T1 coverage) emit JSON null for line
     * fields rather than -1. Reader normalises both shapes back to -1
     * internally so downstream code only handles one missing-value
     * marker.
     */
    private static int readLine(final Object value) {
        return value instanceof Number n ? n.intValue() : -1;
    }
}
