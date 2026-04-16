package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.annotations.SuppressCleanCode;

import java.util.Map;

@SuppressCleanCode(value = HeuristicCode.F1,
        reason = "Finding is the canonical wire/data type; the Builder below is the intended call-site for new code, "
                + "and the long constructor remains for record deserialization and existing callers.")
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

    public static Finding at(final HeuristicCode code, final String sourceFile,
                             final int startLine, final int endLine, final String message,
                             final Severity severity, final Confidence confidence,
                             final String tool, final String ruleRef) {
        return builder()
                .code(code)
                .sourceFile(sourceFile)
                .startLine(startLine)
                .endLine(endLine)
                .message(message)
                .severity(severity)
                .confidence(confidence)
                .tool(tool)
                .ruleRef(ruleRef)
                .build();
    }

    public static Finding projectLevel(final HeuristicCode code, final String message,
                                       final Severity severity, final Confidence confidence,
                                       final String tool, final String ruleRef) {
        return builder()
                .code(code)
                .sourceFile(null)
                .startLine(-1)
                .endLine(-1)
                .message(message)
                .severity(severity)
                .confidence(confidence)
                .tool(tool)
                .ruleRef(ruleRef)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HeuristicCode code;
        private String sourceFile;
        private int startLine = -1;
        private int endLine = -1;
        private String message;
        private Severity severity;
        private Confidence confidence;
        private String tool;
        private String ruleRef;
        private Map<String, String> metadata = Map.of();

        private Builder() {
        }

        public Builder code(final HeuristicCode code) {
            this.code = code;
            return this;
        }

        public Builder sourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder startLine(final int startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(final int endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder severity(final Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder confidence(final Confidence confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder tool(final String tool) {
            this.tool = tool;
            return this;
        }

        public Builder ruleRef(final String ruleRef) {
            this.ruleRef = ruleRef;
            return this;
        }

        public Builder metadata(final Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Finding build() {
            return new Finding(code, sourceFile, startLine, endLine,
                    message, severity, confidence, tool, ruleRef, metadata);
        }
    }
}
