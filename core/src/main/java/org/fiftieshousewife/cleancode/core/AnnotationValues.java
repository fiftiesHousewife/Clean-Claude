package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.HashSet;
import java.util.Set;

final class AnnotationValues {

    private AnnotationValues() {
    }

    static String extractString(final Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return expr.asStringLiteralExpr().getValue();
        }
        if (expr.isBinaryExpr()) {
            final BinaryExpr bin = expr.asBinaryExpr();
            if (bin.getOperator() == BinaryExpr.Operator.PLUS) {
                return extractString(bin.getLeft()) + extractString(bin.getRight());
            }
        }
        return expr.toString();
    }

    static Set<HeuristicCode> extractCodes(final Expression expr) {
        final Set<HeuristicCode> codes = new HashSet<>();
        if (expr.isFieldAccessExpr()) {
            final String name = expr.asFieldAccessExpr().getNameAsString();
            codes.add(HeuristicCode.valueOf(name));
        } else if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(v -> codes.addAll(extractCodes(v)));
        }
        return codes;
    }

    static SuppressionFields extractSuppressionFields(final NormalAnnotationExpr annotation) {
        final Set<HeuristicCode> codes = new HashSet<>();
        final String[] reason = {""};
        final String[] until = {""};
        for (final MemberValuePair pair : annotation.getPairs()) {
            applyPair(pair, codes, reason, until);
        }
        return new SuppressionFields(codes, reason[0], until[0]);
    }

    private static void applyPair(final MemberValuePair pair,
                                  final Set<HeuristicCode> codes,
                                  final String[] reason,
                                  final String[] until) {
        final String name = pair.getNameAsString();
        if ("value".equals(name)) {
            codes.addAll(extractCodes(pair.getValue()));
        } else if ("reason".equals(name)) {
            reason[0] = extractString(pair.getValue());
        } else if ("until".equals(name)) {
            until[0] = extractString(pair.getValue());
        }
    }
}
