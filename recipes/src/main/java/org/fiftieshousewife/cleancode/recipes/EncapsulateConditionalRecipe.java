package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EncapsulateConditionalRecipe extends ScanningRecipe<EncapsulateConditionalRecipe.Accumulator> {

    private static final Set<J.Binary.Type> LOGICAL_OPERATORS =
            Set.of(J.Binary.Type.And, J.Binary.Type.Or);

    public record Row(String className, String methodName, String conditionPreview, int depth, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Encapsulate conditional detection (G28)";
    }

    @Override
    public String getDescription() {
        return "Detects if-statement conditions with more than one logical operator that should be extracted.";
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
                final var condition = i.getIfCondition().getTree();

                if (condition instanceof J.Binary binary) {
                    final int depth = logicalDepth(binary);
                    if (depth >= 2) {
                        acc.rows.add(new Row(
                                findEnclosingClassName(),
                                findEnclosingMethodName(),
                                condition.toString().substring(0, Math.min(80, condition.toString().length())),
                                depth,
                                -1));
                    }
                }

                return i;
            }

            private int logicalDepth(J.Binary binary) {
                if (!LOGICAL_OPERATORS.contains(binary.getOperator())) {
                    return 0;
                }
                int left = binary.getLeft() instanceof J.Binary leftBin ? logicalDepth(leftBin) : 0;
                int right = binary.getRight() instanceof J.Binary rightBin ? logicalDepth(rightBin) : 0;
                return 1 + Math.max(left, right);
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
