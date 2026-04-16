package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

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
}
