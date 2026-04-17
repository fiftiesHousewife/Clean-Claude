package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleAssertRecipe extends ScanningRecipe<MultipleAssertRecipe.Accumulator> {

    private static final int DEFAULT_THRESHOLD = 2;

    private final int threshold;

    public MultipleAssertRecipe(final int threshold) {
        this.threshold = threshold;
    }

    public MultipleAssertRecipe() {
        this(DEFAULT_THRESHOLD);
    }

    public record Row(String className, String methodName, int assertCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Multiple unwrapped assertions detection (T1)";
    }

    @Override
    public String getDescription() {
        return "Detects test methods with consecutive assertions not wrapped in assertAll.";
    }

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new MultipleAssertScanner(acc, threshold);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
