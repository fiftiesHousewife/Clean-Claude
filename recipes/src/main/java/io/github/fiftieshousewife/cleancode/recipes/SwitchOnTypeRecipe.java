package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwitchOnTypeRecipe extends ScanningRecipe<SwitchOnTypeRecipe.Accumulator> {

    private static final int DEPTH_THRESHOLD = 3;

    public record Row(String className, String methodName, int depth, String pattern, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Switch on type detection (G23)";
    }

    @Override
    public String getDescription() {
        return "Detects if/else-if chains of depth >= 3 involving instanceof or type checks.";
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
            public J.If visitIf(J.If ifStatement, ExecutionContext ctx) {
                final J.If i = super.visitIf(ifStatement, ctx);

                if (isPartOfElseIfChain(i)) {
                    return i;
                }

                final int depth = elseIfDepth(i);
                if (depth >= DEPTH_THRESHOLD && involvesTypeCheck(i)) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            findEnclosingMethodName(),
                            depth,
                            "if/else-if chain with type dispatch",
                            -1));
                }

                return i;
            }

            private boolean isPartOfElseIfChain(J.If ifStatement) {
                return getCursor().getParentTreeCursor().getValue() instanceof J.If.Else;
            }

            private int elseIfDepth(J.If ifStatement) {
                int depth = 1;
                var elsePart = ifStatement.getElsePart();
                while (elsePart != null && elsePart.getBody() instanceof J.If nested) {
                    depth++;
                    elsePart = nested.getElsePart();
                }
                return depth;
            }

            private boolean involvesTypeCheck(J.If ifStatement) {
                final String condition = ifStatement.getIfCondition().toString();
                return condition.contains("instanceof") || condition.contains("getClass");
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
