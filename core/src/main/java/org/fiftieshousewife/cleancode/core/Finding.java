package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public record Finding(
        HeuristicCode code,
        String sourceFile,
        int startLine,
        int endLine,
        String message,
        Severity severity,
        Confidence confidence,
        String tool,
        String ruleRef,
        Map<String, String> metadata
) {
    public Finding {
        metadata = Map.copyOf(metadata);
    }

    public static Finding at(HeuristicCode code, String sourceFile,
                             int startLine, int endLine, String message,
                             Severity severity, Confidence confidence,
                             String tool, String ruleRef) {
        return new Finding(code, sourceFile, startLine, endLine,
                message, severity, confidence, tool, ruleRef, Map.of());
    }

    public static Finding projectLevel(HeuristicCode code, String message,
                                       Severity severity, Confidence confidence,
                                       String tool, String ruleRef) {
        return new Finding(code, null, -1, -1, message,
                severity, confidence, tool, ruleRef, Map.of());
    }
}
