package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SuppressionAnnotationReader {

    private static final String TODO_REASON = "TODO";

    private SuppressionAnnotationReader() {}

    record Values(Set<HeuristicCode> codes, String reason, String until) {

        boolean isExpired() {
            if (until.isEmpty()) {
                return false;
            }
            return LocalDate.parse(until).isBefore(LocalDate.now());
        }

        boolean hasNoMeaningfulReason() {
            return reason.isBlank() || TODO_REASON.equalsIgnoreCase(reason.trim());
        }
    }

    static Values read(final AnnotationExpr ann) {
        final Set<HeuristicCode> codes = new HashSet<>();
        String reason = "";
        String until = "";
        if (ann instanceof NormalAnnotationExpr normal) {
            for (final MemberValuePair pair : normal.getPairs()) {
                switch (pair.getNameAsString()) {
                    case "value" -> codes.addAll(extractCodes(pair.getValue()));
                    case "reason" -> reason = extractStringValue(pair.getValue());
                    case "until" -> until = extractStringValue(pair.getValue());
                }
            }
        }
        return new Values(codes, reason, until);
    }

    static List<AnnotationExpr> expandRepeatableContainer(final AnnotationExpr ann) {
        if (!(ann instanceof NormalAnnotationExpr normal)) {
            return List.of();
        }
        for (final MemberValuePair pair : normal.getPairs()) {
            if (!"value".equals(pair.getNameAsString())) {
                continue;
            }
            return pair.getValue().toArrayInitializerExpr()
                    .map(arr -> arr.getValues().stream()
                            .filter(AnnotationExpr.class::isInstance)
                            .map(AnnotationExpr.class::cast)
                            .toList())
                    .orElse(List.of());
        }
        return List.of();
    }

    private static String extractStringValue(final Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return expr.asStringLiteralExpr().getValue();
        }
        if (expr.isBinaryExpr()) {
            final BinaryExpr bin = expr.asBinaryExpr();
            if (bin.getOperator() == BinaryExpr.Operator.PLUS) {
                return extractStringValue(bin.getLeft()) + extractStringValue(bin.getRight());
            }
        }
        return expr.toString();
    }

    private static Set<HeuristicCode> extractCodes(final Expression expr) {
        final Set<HeuristicCode> codes = new HashSet<>();
        if (expr.isFieldAccessExpr()) {
            final String name = expr.asFieldAccessExpr().getNameAsString();
            codes.add(HeuristicCode.valueOf(name));
        } else if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(v -> codes.addAll(extractCodes(v)));
        }
        return codes;
    }
}
