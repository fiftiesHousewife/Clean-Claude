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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureEnvyRecipe extends ScanningRecipe<FeatureEnvyRecipe.Accumulator> {

    static final int MINIMUM_CALL_THRESHOLD = 3;

    public record Row(String className, String methodName, String enviedClass,
                      int selfCallCount, int externalCallCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
        final Set<String> projectTypeNames = new HashSet<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Feature envy detection (G14)";
    }

    @Override
    public String getDescription() {
        return "Detects methods that call more methods on another project class than on their own class.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new FeatureEnvyScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}

final class FeatureEnvyScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final String UNKNOWN_CLASS = "<unknown>";

    private final FeatureEnvyRecipe.Accumulator acc;

    FeatureEnvyScanner(FeatureEnvyRecipe.Accumulator acc) {
        this.acc = acc;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        final String pkg = cu.getPackageDeclaration() != null
                ? cu.getPackageDeclaration().getExpression().toString() : "";
        cu.getClasses().forEach(classDecl -> collectProjectTypes(classDecl, pkg));
        return super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        if (isExcluded(m)) {
            return m;
        }
        final CallCounter counter = new CallCounter();
        new CallClassifier(acc).visit(m, counter);
        if (counter.total() < FeatureEnvyRecipe.MINIMUM_CALL_THRESHOLD) {
            return m;
        }
        counter.externalCalls.entrySet().stream()
                .filter(entry -> entry.getValue() > counter.selfCalls * 2)
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> acc.rows.add(new FeatureEnvyRecipe.Row(enclosingClassName(),
                        m.getSimpleName(), entry.getKey(), counter.selfCalls, entry.getValue(), -1)));
        return m;
    }

    private boolean isExcluded(J.MethodDeclaration m) {
        return isConstructor(m) || isStatic(m) || isVisitorMethod(m) || isInTestClass();
    }

    private void collectProjectTypes(J.ClassDeclaration classDecl, String pkg) {
        final String fqn = pkg.isEmpty() ? classDecl.getSimpleName() : pkg + "." + classDecl.getSimpleName();
        acc.projectTypeNames.add(fqn);
        classDecl.getBody().getStatements().stream()
                .filter(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .forEach(inner -> collectProjectTypes(inner, fqn));
    }

    private static boolean isVisitorMethod(J.MethodDeclaration m) {
        return m.getSimpleName().startsWith("visit");
    }

    private static boolean isConstructor(J.MethodDeclaration m) {
        return m.getMethodType() != null && m.getMethodType().isConstructor();
    }

    private static boolean isStatic(J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
    }

    private boolean isInTestClass() {
        final J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return c != null && c.getSimpleName().endsWith("Test");
    }

    private String enclosingClassName() {
        final J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return c != null ? c.getSimpleName() : UNKNOWN_CLASS;
    }
}

final class CallClassifier extends JavaIsoVisitor<CallCounter> {

    private static final String THIS = "this";

    private final FeatureEnvyRecipe.Accumulator acc;

    CallClassifier(FeatureEnvyRecipe.Accumulator acc) {
        this.acc = acc;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, CallCounter counter) {
        final J.MethodInvocation mi = super.visitMethodInvocation(method, counter);
        final Expression select = mi.getSelect();
        if (isSelfCall(select)) {
            counter.selfCalls++;
        } else if (select instanceof J.Identifier id && isProjectType(id)) {
            counter.externalCalls.merge(resolveTypeName(id), 1, Integer::sum);
        }
        return mi;
    }

    private static boolean isSelfCall(Expression select) {
        return select == null || (select instanceof J.Identifier id && THIS.equals(id.getSimpleName()));
    }

    private boolean isProjectType(J.Identifier id) {
        return id.getType() instanceof JavaType.FullyQualified fq
                && acc.projectTypeNames.contains(fq.getFullyQualifiedName());
    }

    private static String resolveTypeName(J.Identifier id) {
        return id.getType() instanceof JavaType.FullyQualified fq ? fq.getClassName() : id.getSimpleName();
    }
}

final class CallCounter {
    int selfCalls;
    final Map<String, Integer> externalCalls = new HashMap<>();

    int total() {
        return selfCalls + externalCalls.values().stream().mapToInt(Integer::intValue).sum();
    }
}
