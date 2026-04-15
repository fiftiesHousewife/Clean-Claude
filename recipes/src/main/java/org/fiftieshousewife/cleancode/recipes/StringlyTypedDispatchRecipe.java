package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StringlyTypedDispatchRecipe extends ScanningRecipe<StringlyTypedDispatchRecipe.Accumulator> {

    private static final Set<String> SKIP_METHODS = Set.of("equals", "hashCode", "toString", "main");

    public record Row(String className, String methodName, String parameterName, int branchCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Stringly-typed dispatch detection";
    }

    @Override
    public String getDescription() {
        return "Detects methods that use a String parameter to select behaviour via switch or if/else chains.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (SKIP_METHODS.contains(m.getSimpleName()) || m.getBody() == null) {
                    return m;
                }

                final Set<String> stringParamNames = m.getParameters().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .filter(StringlyTypedDispatchRecipe::isStringType)
                        .flatMap(v -> v.getVariables().stream())
                        .map(v -> v.getSimpleName())
                        .collect(Collectors.toSet());

                if (stringParamNames.isEmpty()) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                final String className = enclosingClass != null ? enclosingClass.getSimpleName() : "<unknown>";

                stringParamNames.forEach(paramName -> {
                    final int switchBranches = countSwitchBranches(m.getBody(), paramName);
                    if (switchBranches >= 2) {
                        acc.rows.add(new Row(className, m.getSimpleName(), paramName, switchBranches));
                        return;
                    }

                    final int ifBranches = countIfElseBranches(m.getBody(), paramName);
                    if (ifBranches >= 2) {
                        acc.rows.add(new Row(className, m.getSimpleName(), paramName, ifBranches));
                    }
                });

                return m;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static boolean isStringType(J.VariableDeclarations varDecls) {
        if (varDecls.getType() instanceof JavaType.FullyQualified fq) {
            return "java.lang.String".equals(fq.getFullyQualifiedName());
        }
        return varDecls.getTypeExpression() != null
                && "String".equals(varDecls.getTypeExpression().toString().trim());
    }

    private static int countSwitchBranches(J.Block body, String paramName) {
        final List<Integer> counts = new ArrayList<>();
        new JavaIsoVisitor<List<Integer>>() {
            @Override
            public J.Switch visitSwitch(J.Switch sw, List<Integer> out) {
                if (selectorReferencesParam(sw.getSelector(), paramName)) {
                    final int caseCount = (int) sw.getCases().getStatements().stream()
                            .filter(J.Case.class::isInstance)
                            .count();
                    out.add(caseCount);
                }
                return super.visitSwitch(sw, out);
            }
        }.visit(body, counts);
        return counts.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static boolean selectorReferencesParam(
            J.ControlParentheses<org.openrewrite.java.tree.Expression> selector, String paramName) {
        final String selectorText = selector.getTree().toString().trim();
        return selectorText.equals(paramName);
    }

    private static int countIfElseBranches(J.Block body, String paramName) {
        final List<Integer> counts = new ArrayList<>();
        new JavaIsoVisitor<List<Integer>>() {
            @Override
            public J.If visitIf(J.If iff, List<Integer> out) {
                final int branches = countChainedEquals(iff, paramName);
                if (branches >= 2) {
                    out.add(branches);
                }
                return super.visitIf(iff, out);
            }
        }.visit(body, counts);
        return counts.stream().mapToInt(Integer::intValue).max().orElse(0);
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

    private static boolean conditionReferencesParamEquals(J.ControlParentheses<org.openrewrite.java.tree.Expression> cond,
                                                          String paramName) {
        final String condStr = cond.toString().trim();
        return condStr.contains(paramName + ".equals(") || condStr.contains(".equals(" + paramName + ")");
    }
}
