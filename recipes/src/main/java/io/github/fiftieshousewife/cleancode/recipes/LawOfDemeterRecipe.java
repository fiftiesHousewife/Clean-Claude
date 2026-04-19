package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LawOfDemeterRecipe extends ScanningRecipe<LawOfDemeterRecipe.Accumulator> {

    private static final int DEFAULT_CHAIN_DEPTH_THRESHOLD = 3;

    private final int chainDepthThreshold;

    public LawOfDemeterRecipe() {
        this(DEFAULT_CHAIN_DEPTH_THRESHOLD);
    }

    public LawOfDemeterRecipe(final int chainDepthThreshold) {
        this.chainDepthThreshold = chainDepthThreshold;
    }

    private static final Set<String> FLUENT_METHOD_NAMES = Set.of(
            "append", "add", "put", "set", "with", "and", "or",
            "builder", "build", "create", "of", "from",
            "stream", "filter", "map", "flatMap", "collect", "reduce",
            "sorted", "distinct", "limit", "skip", "peek",
            "forEach", "toList", "toSet", "toMap",
            "findFirst", "findAny", "anyMatch", "allMatch", "noneMatch",
            "count", "min", "max", "sum", "average",
            "orElse", "orElseGet", "orElseThrow",
            "ifPresent", "ifPresentOrElse",
            "thenApply", "thenCompose", "thenAccept", "thenRun",
            "whenComplete", "exceptionally",
            "configure", "configureEach", "register", "named",
            "resolve", "toUri", "toPath", "toFile",
            "getRequired", "getAsFile",
            "logCompilationWarningsAndErrors",
            "parse", "replace", "replaceAll", "trim", "strip",
            "toLowerCase", "toUpperCase", "substring",
            "format", "formatted",
            "mapToInt", "mapToLong", "mapToDouble");

    public record Row(String className, String methodName, String chain, int depth, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
        final Set<String> projectTypeNames = new HashSet<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Law of Demeter violation detection (G36)";
    }

    @Override
    public String getDescription() {
        return "Detects method invocation chains through project types of depth >= 3.";
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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (isPartOfChain(m)) {
                    return m;
                }

                final int depth = chainDepth(m);
                if (depth >= chainDepthThreshold
                        && !isFluentChain(m)
                        && involvesProjectTypes(m, acc)) {
                    final String chain = buildChainString(m);
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            findEnclosingMethodName(),
                            chain,
                            depth,
                            -1));
                }

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

            private boolean involvesProjectTypes(J.MethodInvocation invocation, Accumulator acc) {
                var current = invocation;
                while (current != null) {
                    final JavaType returnType = current.getType();
                    if (returnType instanceof JavaType.FullyQualified fq
                            && acc.projectTypeNames.contains(fq.getFullyQualifiedName())) {
                        return true;
                    }
                    current = current.getSelect() instanceof J.MethodInvocation nested ? nested : null;
                }
                final var root = findChainRoot(invocation);
                if (root != null && root.getType() instanceof JavaType.FullyQualified fq) {
                    return acc.projectTypeNames.contains(fq.getFullyQualifiedName());
                }
                return false;
            }

            private J.Identifier findChainRoot(J.MethodInvocation invocation) {
                var current = invocation.getSelect();
                while (current instanceof J.MethodInvocation nested) {
                    current = nested.getSelect();
                }
                return current instanceof J.Identifier id ? id : null;
            }

            private boolean isFluentChain(J.MethodInvocation invocation) {
                int total = 0;
                int fluent = 0;
                var current = invocation;
                while (current != null) {
                    total++;
                    if (FLUENT_METHOD_NAMES.contains(current.getSimpleName())) {
                        fluent++;
                    }
                    current = current.getSelect() instanceof J.MethodInvocation nested ? nested : null;
                }
                return fluent > total / 2;
            }

            private boolean isPartOfChain(J.MethodInvocation method) {
                return getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation;
            }

            private int chainDepth(J.MethodInvocation invocation) {
                int depth = 1;
                var current = invocation.getSelect();
                while (current instanceof J.MethodInvocation nested) {
                    depth++;
                    current = nested.getSelect();
                }
                return depth;
            }

            private String buildChainString(J.MethodInvocation invocation) {
                final var parts = new ArrayList<String>();
                parts.add(invocation.getSimpleName() + "()");
                var current = invocation.getSelect();
                while (current instanceof J.MethodInvocation nested) {
                    parts.addFirst(nested.getSimpleName() + "()");
                    current = nested.getSelect();
                }
                if (current instanceof J.Identifier identifier) {
                    parts.addFirst(identifier.getSimpleName());
                }
                return String.join(".", parts);
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }

            private String findEnclosingMethodName() {
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return methodDecl != null ? methodDecl.getSimpleName() : "<unknown>";
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
}
