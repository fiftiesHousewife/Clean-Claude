package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
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

    private static final int MINIMUM_CALL_THRESHOLD = 3;

    public record Row(
            String className,
            String methodName,
            String enviedClass,
            int selfCallCount,
            int externalCallCount,
            int lineNumber
    ) {}

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
        return new JavaIsoVisitor<>() {

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                cu.getClasses().forEach(classDecl ->
                        collectProjectTypes(classDecl, packagePrefix(cu), acc));
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (isConstructor(m) || isStatic(m) || isInTestClass()
                        || isVisitorMethod(m)) {
                    return m;
                }

                final CallCounter counter = new CallCounter();
                new JavaIsoVisitor<CallCounter>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(
                            J.MethodInvocation method, CallCounter callCounter) {
                        final J.MethodInvocation mi = super.visitMethodInvocation(method, callCounter);
                        final Expression select = mi.getSelect();

                        if (select == null || select instanceof J.Identifier id
                                && "this".equals(id.getSimpleName())) {
                            callCounter.selfCalls++;
                        } else if (select instanceof J.Identifier id && isProjectType(id, acc)) {
                            final String typeName = resolveTypeName(id);
                            callCounter.externalCalls.merge(typeName, 1, Integer::sum);
                        }

                        return mi;
                    }
                }.visit(m, counter);

                final int totalCalls = counter.selfCalls
                        + counter.externalCalls.values().stream().mapToInt(Integer::intValue).sum();
                if (totalCalls < MINIMUM_CALL_THRESHOLD) {
                    return m;
                }

                counter.externalCalls.entrySet().stream()
                        .filter(entry -> entry.getValue() > counter.selfCalls * 2)
                        .max(Map.Entry.comparingByValue())
                        .ifPresent(entry -> acc.rows.add(new Row(
                                findEnclosingClassName(),
                                m.getSimpleName(),
                                entry.getKey(),
                                counter.selfCalls,
                                entry.getValue(),
                                -1)));

                return m;
            }

            private void collectProjectTypes(J.ClassDeclaration classDecl, String pkg,
                                             Accumulator acc) {
                final String fqn = pkg.isEmpty()
                        ? classDecl.getSimpleName()
                        : pkg + "." + classDecl.getSimpleName();
                acc.projectTypeNames.add(fqn);
                classDecl.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.ClassDeclaration)
                        .map(s -> (J.ClassDeclaration) s)
                        .forEach(inner -> collectProjectTypes(inner, fqn, acc));
            }

            private String packagePrefix(J.CompilationUnit cu) {
                return cu.getPackageDeclaration() != null
                        ? cu.getPackageDeclaration().getExpression().toString()
                        : "";
            }

            private boolean isProjectType(J.Identifier id, Accumulator acc) {
                final JavaType type = id.getType();
                if (type instanceof JavaType.FullyQualified fq) {
                    return acc.projectTypeNames.contains(fq.getFullyQualifiedName());
                }
                return false;
            }

            private String resolveTypeName(J.Identifier id) {
                final JavaType type = id.getType();
                if (type instanceof JavaType.FullyQualified fq) {
                    return fq.getClassName();
                }
                return id.getSimpleName();
            }

            private boolean isVisitorMethod(J.MethodDeclaration method) {
                return method.getSimpleName().startsWith("visit");
            }

            private boolean isConstructor(J.MethodDeclaration method) {
                return method.getMethodType() != null && method.getMethodType().isConstructor();
            }

            private boolean isStatic(J.MethodDeclaration method) {
                return method.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
            }

            private boolean isInTestClass() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null && classDecl.getSimpleName().endsWith("Test");
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
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

    private static class CallCounter {
        int selfCalls;
        final Map<String, Integer> externalCalls = new HashMap<>();
    }
}
