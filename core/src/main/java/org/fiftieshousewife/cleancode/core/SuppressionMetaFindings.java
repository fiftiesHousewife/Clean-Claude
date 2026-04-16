package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.expr.AnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

final class SuppressionMetaFindings {

    private static final String TOOL = "suppression-index";

    private final List<Finding> sink;

    SuppressionMetaFindings(final List<Finding> sink) {
        this.sink = sink;
    }

    void recordIfExpired(final AnnotationExpr ann, final String sourceFile,
                         final Set<HeuristicCode> codes, final String until) {
        if (until.isEmpty()) {
            return;
        }
        final LocalDate untilDate = LocalDate.parse(until);
        if (!untilDate.isBefore(LocalDate.now())) {
            return;
        }
        sink.add(Finding.at(
                HeuristicCode.META_SUPPRESSION_EXPIRED, sourceFile,
                ann.getBegin().map(p -> p.line).orElse(-1),
                ann.getEnd().map(p -> p.line).orElse(-1),
                "Suppression expired on " + until + " for " + codes,
                Severity.ERROR, Confidence.HIGH,
                TOOL, "expired-suppression"));
    }

    void recordIfBlankReason(final AnnotationExpr ann, final String sourceFile, final String reason) {
        if (!reason.isBlank() && !"TODO".equalsIgnoreCase(reason.trim())) {
            return;
        }
        sink.add(Finding.at(
                HeuristicCode.META_SUPPRESSION_NO_REASON, sourceFile,
                ann.getBegin().map(p -> p.line).orElse(-1),
                ann.getEnd().map(p -> p.line).orElse(-1),
                "Suppression has no meaningful reason: '" + reason + "'",
                Severity.WARNING, Confidence.HIGH,
                TOOL, "blank-reason"));
    }
}
