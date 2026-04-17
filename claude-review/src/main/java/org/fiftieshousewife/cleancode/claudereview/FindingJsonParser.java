package org.fiftieshousewife.cleancode.claudereview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

final class FindingJsonParser {

    private static final String TOOL = "claude-review";
    private static final Logger LOG = Logger.getLogger(FindingJsonParser.class.getName());

    private final Set<HeuristicCode> enabledCodes;
    private final String model;

    FindingJsonParser(final ClaudeReviewConfig config) {
        this.enabledCodes = config.enabledCodes();
        this.model = config.model();
    }

    List<Finding> parse(final String json, final String sourceFile) {
        try {
            final JsonArray array = JsonParser.parseString(unwrapArray(json)).getAsJsonArray();
            final List<Finding> findings = new ArrayList<>();
            for (final JsonElement element : array) {
                toFinding(element.getAsJsonObject(), sourceFile).ifPresent(findings::add);
            }
            return findings;
        } catch (JsonParseException | IllegalStateException e) {
            LOG.log(Level.WARNING, "Failed to parse Claude response as JSON: " + e.getMessage());
            return List.of();
        }
    }

    private Optional<Finding> toFinding(final JsonObject obj, final String sourceFile) {
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
                    TOOL, codeStr, Map.of("model", model)));
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Unknown heuristic code in response: " + codeStr);
            return Optional.empty();
        }
    }

    static String unwrapArray(final String json) {
        final String cleaned = json.strip();
        if (cleaned.startsWith("[")) {
            return cleaned;
        }
        final int start = cleaned.indexOf('[');
        final int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return "[]";
    }
}
