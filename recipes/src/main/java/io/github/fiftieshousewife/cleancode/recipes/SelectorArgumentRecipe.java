package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectorArgumentRecipe extends ScanningRecipe<SelectorArgumentRecipe.Accumulator> {

    public record Row(String className, String methodName, String parameterName, String parameterType) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Selector argument detection (G15)";
    }

    @Override
    public String getDescription() {
        return "Detects methods with boolean or enum parameters used to select behaviour via if or switch.";
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
                if (m.getBody() == null) {
                    return m;
                }

                final Set<String> selectorParams = m.getParameters().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .filter(v -> isBooleanOrEnum(v))
                        .flatMap(v -> v.getVariables().stream())
                        .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                        .collect(Collectors.toSet());

                if (selectorParams.isEmpty()) {
                    return m;
                }

                final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                final String className = enclosing != null ? enclosing.getSimpleName() : "<unknown>";

                selectorParams.forEach(param -> {
                    if (isUsedAsSelector(m.getBody(), param)) {
                        final String paramType = findParamType(m, param);
                        acc.rows.add(new Row(className, m.getSimpleName(), param, paramType));
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

    private static boolean isBooleanOrEnum(J.VariableDeclarations varDecl) {
        if (varDecl.getType() instanceof JavaType.Primitive p) {
            return p == JavaType.Primitive.Boolean;
        }
        final String typeText = varDecl.getTypeExpression() != null
                ? varDecl.getTypeExpression().toString().trim() : "";
        return "boolean".equals(typeText) || isLikelyEnum(varDecl);
    }

    private static boolean isLikelyEnum(J.VariableDeclarations varDecl) {
        if (varDecl.getType() instanceof JavaType.FullyQualified fq) {
            return fq.getKind() == JavaType.FullyQualified.Kind.Enum;
        }
        return false;
    }

    private static boolean isUsedAsSelector(J.Block body, String paramName) {
        final List<Boolean> found = new ArrayList<>();
        new JavaIsoVisitor<List<Boolean>>() {
            @Override
            public J.If visitIf(J.If iff, List<Boolean> out) {
                if (conditionReferencesParam(iff.getIfCondition().getTree(), paramName)) {
                    out.add(true);
                }
                return super.visitIf(iff, out);
            }

            @Override
            public J.Switch visitSwitch(J.Switch sw, List<Boolean> out) {
                if (selectorReferencesParam(sw.getSelector().getTree(), paramName)) {
                    out.add(true);
                }
                return super.visitSwitch(sw, out);
            }
        }.visit(body, found);
        return !found.isEmpty();
    }

    private static boolean conditionReferencesParam(Expression condition, String paramName) {
        final String condText = condition.toString().trim();
        return condText.equals(paramName) || condText.startsWith(paramName + " ")
                || condText.startsWith("!" + paramName);
    }

    private static boolean selectorReferencesParam(Expression selector, String paramName) {
        return selector.toString().trim().equals(paramName);
    }

    private static String findParamType(J.MethodDeclaration method, String paramName) {
        return method.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .filter(v -> v.getVariables().stream().anyMatch(n -> n.getSimpleName().equals(paramName)))
                .map(v -> v.getTypeExpression() != null ? v.getTypeExpression().toString().trim() : "unknown")
                .findFirst()
                .orElse("unknown");
    }
}
