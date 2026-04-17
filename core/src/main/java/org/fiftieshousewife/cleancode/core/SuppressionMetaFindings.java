package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

final class SuppressionMetaFindings {

    private static final String TOOL = "suppression-index";
    private static final String TODO = "TODO";

    private final List<Finding> sink;

    SuppressionMetaFindings(final List<Finding> sink) {
        this.sink = sink;
    }

    void recordIfExpired(final AnnotationExpr ann, final String sourceFile,
                         final Set<HeuristicCode> codes, final String until) {
        if (!isExpired(until)) {
            return;
        }
        final int beginLine = beginLineOf(ann);
        final int endLine = endLineOf(ann);
        sink.add(Finding.at(
                HeuristicCode.META_SUPPRESSION_EXPIRED, sourceFile,
                beginLine, endLine,
                "Suppression expired on " + until + " for " + codes,
                Severity.ERROR, Confidence.HIGH,
                TOOL, "expired-suppression"));
    }

    void recordIfBlankReason(final AnnotationExpr ann, final String sourceFile, final String reason) {
        if (isMeaningfulReason(reason)) {
            return;
        }
        final int beginLine = beginLineOf(ann);
        final int endLine = endLineOf(ann);
        sink.add(Finding.at(
                HeuristicCode.META_SUPPRESSION_NO_REASON, sourceFile,
                beginLine, endLine,
                "Suppression has no meaningful reason: '" + reason + "'",
                Severity.WARNING, Confidence.HIGH,
                TOOL, "blank-reason"));
    }

    private static boolean isExpired(final String until) {
        if (until.isEmpty()) {
            return false;
        }
        final LocalDate untilDate = LocalDate.parse(until);
        return untilDate.isBefore(LocalDate.now());
    }

    private static boolean isMeaningfulReason(final String reason) {
        return !reason.isBlank() && !TODO.equalsIgnoreCase(reason.trim());
    }

    private static int beginLineOf(final Node node) {
        return node.getBegin().map(p -> p.line).orElse(-1);
    }

    private static int endLineOf(final Node node) {
        return node.getEnd().map(p -> p.line).orElse(-1);
    }
}
