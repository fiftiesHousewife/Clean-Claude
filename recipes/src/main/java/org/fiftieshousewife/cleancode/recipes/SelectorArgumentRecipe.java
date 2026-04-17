package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectorArgumentRecipe extends ScanningRecipe<SelectorArgumentRecipe.Accumulator> {

    private static final String UNKNOWN = "unknown";

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
        return new SelectorScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private final class SelectorScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        SelectorScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            final Set<String> selectorParams = findSelectorParams(m);
            if (selectorParams.isEmpty()) {
                return m;
            }
            recordSelectorUsages(m, selectorParams);
            return m;
        }

        private void recordSelectorUsages(final J.MethodDeclaration m, final Set<String> selectorParams) {
            final String className = enclosingClassName();
            selectorParams.stream()
                    .filter(param -> isUsedAsSelector(m.getBody(), param))
                    .forEach(param -> acc.rows.add(new Row(className, m.getSimpleName(), param, findParamType(m, param))));
        }

        private String enclosingClassName() {
            final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return enclosing != null ? enclosing.getSimpleName() : "<unknown>";
        }
    }

    private static Set<String> findSelectorParams(final J.MethodDeclaration m) {
        if (m.getBody() == null) {
            return Set.of();
        }
        return m.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .filter(SelectorArgumentRecipe::isBooleanOrEnum)
                .flatMap(v -> v.getVariables().stream())
                .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                .collect(Collectors.toSet());
    }

    private static boolean isBooleanOrEnum(final J.VariableDeclarations varDecl) {
        if (varDecl.getType() instanceof JavaType.Primitive p) {
            return p == JavaType.Primitive.Boolean;
        }
        final String typeText = varDecl.getTypeExpression() != null
                ? varDecl.getTypeExpression().toString().trim() : "";
        return "boolean".equals(typeText) || isLikelyEnum(varDecl);
    }

    private static boolean isLikelyEnum(final J.VariableDeclarations varDecl) {
        if (varDecl.getType() instanceof JavaType.FullyQualified fq) {
            return fq.getKind() == JavaType.FullyQualified.Kind.Enum;
        }
        return false;
    }

    private static boolean isUsedAsSelector(final J.Block body, final String paramName) {
        final SelectorUsageDetector detector = new SelectorUsageDetector(paramName);
        detector.visit(body, 0);
        return detector.found;
    }

    private static final class SelectorUsageDetector extends JavaIsoVisitor<Integer> {
        private final String paramName;
        boolean found;

        SelectorUsageDetector(final String paramName) {
            this.paramName = paramName;
        }

        @Override
        public J.If visitIf(final J.If iff, final Integer unused) {
            if (conditionReferencesParam(iff.getIfCondition().getTree(), paramName)) {
                found = true;
            }
            return super.visitIf(iff, unused);
        }

        @Override
        public J.Switch visitSwitch(final J.Switch sw, final Integer unused) {
            if (selectorReferencesParam(sw.getSelector().getTree(), paramName)) {
                found = true;
            }
            return super.visitSwitch(sw, unused);
        }
    }

    private static boolean conditionReferencesParam(final Expression condition, final String paramName) {
        final String condText = condition.toString().trim();
        return condText.equals(paramName) || condText.startsWith(paramName + " ")
                || condText.startsWith("!" + paramName);
    }

    private static boolean selectorReferencesParam(final Expression selector, final String paramName) {
        return selector.toString().trim().equals(paramName);
    }

    private static String findParamType(final J.MethodDeclaration method, final String paramName) {
        return method.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .filter(v -> v.getVariables().stream().anyMatch(n -> n.getSimpleName().equals(paramName)))
                .map(v -> v.getTypeExpression() != null ? v.getTypeExpression().toString().trim() : UNKNOWN)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
