package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    public static AggregatedReport read(final Path inputFile) throws IOException {
        final String json = Files.readString(inputFile);
        final Gson gson = new Gson();
        final Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());

        final String projectName = (String) raw.get("projectName");
        final String projectVersion = (String) raw.get("projectVersion");
        final Instant generatedAt = Instant.parse((String) raw.get("generatedAt"));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> rawFindings = (List<Map<String, Object>>) raw.get("findings");

        final List<Finding> findings = new ArrayList<>();
        final Set<HeuristicCode> coveredCodes = EnumSet.noneOf(HeuristicCode.class);

        for (final Map<String, Object> rf : rawFindings) {
            final HeuristicCode code = HeuristicCode.valueOf((String) rf.get("code"));
            coveredCodes.add(code);

            final String sourceFile = (String) rf.get("sourceFile");
            final int startLine = ((Double) rf.get("startLine")).intValue();
            final int endLine = ((Double) rf.get("endLine")).intValue();
            final String message = (String) rf.get("message");
            final Severity severity = Severity.valueOf((String) rf.get("severity"));
            final Confidence confidence = Confidence.valueOf((String) rf.get("confidence"));
            final String tool = (String) rf.get("tool");
            final String ruleRef = (String) rf.get("ruleRef");

            @SuppressWarnings("unchecked")
            final Map<String, String> metadata = rf.get("metadata") != null
                    ? (Map<String, String>) rf.get("metadata")
                    : Map.of();

            findings.add(new Finding(code, sourceFile, startLine, endLine,
                    message, severity, confidence, tool, ruleRef, metadata));
        }

        return new AggregatedReport(findings, coveredCodes, generatedAt, projectName, projectVersion);
    }
}
