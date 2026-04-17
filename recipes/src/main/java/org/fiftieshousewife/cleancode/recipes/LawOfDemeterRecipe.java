package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;

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
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new LawOfDemeterScanner(chainDepthThreshold, acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
