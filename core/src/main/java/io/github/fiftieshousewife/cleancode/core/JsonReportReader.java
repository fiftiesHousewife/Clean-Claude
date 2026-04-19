package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class JsonReportReader {

    private JsonReportReader() {}

    public static AggregatedReport read(Path inputFile) throws IOException {
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
            int startLine = ((Double) rf.get("startLine")).intValue();
            int endLine = ((Double) rf.get("endLine")).intValue();
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
}
