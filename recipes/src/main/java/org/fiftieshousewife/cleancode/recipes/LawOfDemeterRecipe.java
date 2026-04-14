package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LawOfDemeterRecipe extends ScanningRecipe<LawOfDemeterRecipe.Accumulator> {

    private static final int CHAIN_DEPTH_THRESHOLD = 3;

    public record Row(String className, String methodName, String chain, int depth, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Law of Demeter violation detection (G36)";
    }

    @Override
    public String getDescription() {
        return "Detects method invocation chains of depth >= 3.";
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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (isPartOfChain(m)) {
                    return m;
                }

                final int depth = chainDepth(m);
                if (depth >= CHAIN_DEPTH_THRESHOLD) {
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
