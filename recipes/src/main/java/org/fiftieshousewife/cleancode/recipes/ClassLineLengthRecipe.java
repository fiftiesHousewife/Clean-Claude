package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassLineLengthRecipe extends ScanningRecipe<ClassLineLengthRecipe.Accumulator> {

    private static final int COMPOSITE_THRESHOLD = 150;
    private static final int STANDALONE_THRESHOLD = 300;

    public record Row(String className, int lineCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Class line length detection (Ch10.1/Ch10.2)";
    }

    @Override
    public String getDescription() {
        return "Detects classes exceeding line count thresholds.";
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
                final int lineCount = computeLineCount(c);

                if (lineCount > COMPOSITE_THRESHOLD) {
                    acc.rows.add(new Row(c.getSimpleName(), lineCount, -1));
                }

                return c;
            }

            private int computeLineCount(J.ClassDeclaration classDecl) {
                final String source = classDecl.print(getCursor());
                return (int) source.lines().count();
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

    int compositeThreshold() {
        return COMPOSITE_THRESHOLD;
    }

    int standaloneThreshold() {
        return STANDALONE_THRESHOLD;
    }
}
