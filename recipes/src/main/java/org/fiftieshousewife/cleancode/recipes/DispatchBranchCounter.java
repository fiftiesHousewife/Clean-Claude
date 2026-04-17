package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

final class DispatchBranchCounter {

    private static final String EQUALS_CALL = ".equals(";
    private static final int MIN_BRANCHES = 2;

    private DispatchBranchCounter() {}

    static int countSwitchBranches(J.Block body, String paramName) {
        final List<Integer> counts = new ArrayList<>();
        new JavaIsoVisitor<List<Integer>>() {
            @Override
            public J.Switch visitSwitch(J.Switch sw, List<Integer> out) {
                if (selectorReferencesParam(sw.getSelector(), paramName)) {
                    out.add(caseCount(sw));
                }
                return super.visitSwitch(sw, out);
            }
        }.visit(body, counts);
        return maxOrZero(counts);
    }

    static int countIfElseBranches(J.Block body, String paramName) {
        final List<Integer> counts = new ArrayList<>();
        new JavaIsoVisitor<List<Integer>>() {
            @Override
            public J.If visitIf(J.If iff, List<Integer> out) {
                final int branches = countChainedEquals(iff, paramName);
                if (branches >= MIN_BRANCHES) {
                    out.add(branches);
                }
                return super.visitIf(iff, out);
            }
        }.visit(body, counts);
        return maxOrZero(counts);
    }

    private static int maxOrZero(List<Integer> counts) {
        return counts.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static int caseCount(J.Switch sw) {
        return (int) sw.getCases().getStatements().stream()
                .filter(J.Case.class::isInstance)
                .count();
    }

    private static boolean selectorReferencesParam(J.ControlParentheses<Expression> selector, String paramName) {
        return selector.getTree().toString().trim().equals(paramName);
    }

    private static int countChainedEquals(J.If iff, String paramName) {
        int count = 0;
        J.If current = iff;
        while (current != null) {
            if (conditionReferencesParamEquals(current.getIfCondition(), paramName)) {
                count++;
            }
            if (current.getElsePart() != null && current.getElsePart().getBody() instanceof J.If nextIf) {
                current = nextIf;
            } else {
                break;
            }
        }
        return count;
    }

    private static boolean conditionReferencesParamEquals(
            J.ControlParentheses<Expression> cond, String paramName) {
        final String condStr = cond.toString().trim();
        return condStr.contains(paramName + EQUALS_CALL) || condStr.contains(EQUALS_CALL + paramName + ")");
    }
}
