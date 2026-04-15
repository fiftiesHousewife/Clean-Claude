package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LargeConstructorRecipe extends ScanningRecipe<LargeConstructorRecipe.Accumulator> {

    private final int threshold;

    public LargeConstructorRecipe(int threshold) {
        this.threshold = threshold;
    }

    public LargeConstructorRecipe() {
        this(6);
    }

    public record Row(String className, int parameterCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Large constructor detection (F1)";
    }

    @Override
    public String getDescription() {
        return "Detects constructors with too many parameters — introduce a parameter object or builder.";
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
                if (!m.isConstructor()) {
                    return m;
                }
                final int paramCount = m.getParameters().stream()
                        .filter(p -> p instanceof J.VariableDeclarations)
                        .mapToInt(p -> 1)
                        .sum();
                if (paramCount > threshold) {
                    final String className = findEnclosingClassName();
                    acc.rows.add(new Row(className, paramCount));
                }
                return m;
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
}
