package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LargeRecordRecipe extends ScanningRecipe<LargeRecordRecipe.Accumulator> {

    private static final int DEFAULT_COMPONENT_THRESHOLD = 4;

    private final int componentThreshold;

    public LargeRecordRecipe() {
        this(DEFAULT_COMPONENT_THRESHOLD);
    }

    public LargeRecordRecipe(final int componentThreshold) {
        this.componentThreshold = componentThreshold;
    }

    public record Row(String className, int componentCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Large record without builder detection (Ch10.2)";
    }

    @Override
    public String getDescription() {
        return "Detects records with more than " + componentThreshold + " components and no nested Builder class.";
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
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getKind() != J.ClassDeclaration.Kind.Type.Record) {
                    return c;
                }

                final int componentCount = countRecordComponents(c);
                if (componentCount > componentThreshold && !hasNestedBuilder(c)) {
                    acc.rows.add(new Row(c.getSimpleName(), componentCount, -1));
                }

                return c;
            }

            private int countRecordComponents(J.ClassDeclaration record) {
                if (record.getPrimaryConstructor() == null) {
                    return 0;
                }
                return (int) record.getPrimaryConstructor().stream()
                        .filter(s -> s instanceof J.VariableDeclarations)
                        .count();
            }

            private boolean hasNestedBuilder(J.ClassDeclaration record) {
                return record.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.ClassDeclaration)
                        .map(s -> (J.ClassDeclaration) s)
                        .anyMatch(inner -> "Builder".equals(inner.getSimpleName()));
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
