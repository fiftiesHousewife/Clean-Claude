package org.fiftieshousewife.cleancode.claudereview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClaudeFindingParser {

    private static final Logger LOG = Logger.getLogger(ClaudeFindingParser.class.getName());

    private final Set<HeuristicCode> enabledCodes;
    private final String tool;
    private final String model;

    ClaudeFindingParser(Set<HeuristicCode> enabledCodes, String tool, String model) {
        this.enabledCodes = enabledCodes;
        this.tool = tool;
        this.model = model;
    }

    List<Finding> parse(String json, String sourceFile) {
        try {
            final String cleaned = json.strip();
            final String toParse = cleaned.startsWith("[") ? cleaned : extractJsonArray(cleaned);
            final JsonArray array = JsonParser.parseString(toParse).getAsJsonArray();
            final List<Finding> findings = new ArrayList<>();
            for (final JsonElement element : array) {
                toFinding(element.getAsJsonObject(), sourceFile).ifPresent(findings::add);
            }
            return findings;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse Claude response as JSON: " + e.getMessage());
            return List.of();
        }
    }

    private Optional<Finding> toFinding(JsonObject obj, String sourceFile) {
        final String codeStr = obj.get("code").getAsString();
        final int startLine = obj.get("startLine").getAsInt();
        final int endLine = obj.get("endLine").getAsInt();
        final String message = obj.get("message").getAsString();
        try {
            final HeuristicCode code = HeuristicCode.valueOf(codeStr);
            if (!enabledCodes.contains(code) || startLine < 1) {
                return Optional.empty();
            }
            return Optional.of(new Finding(
                    code, sourceFile, startLine, endLine,
                    message, Severity.WARNING, Confidence.LOW,
                    tool, codeStr, Map.of("model", model)));
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Unknown heuristic code in response: " + codeStr);
            return Optional.empty();
        }
    }

    private static String extractJsonArray(String text) {
        final int start = text.indexOf('[');
        final int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "[]";
    }
}
