package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhitespaceSplitMethodRecipe extends ScanningRecipe<WhitespaceSplitMethodRecipe.Accumulator> {

    private static final int DEFAULT_BLANK_LINE_THRESHOLD = 2;
    private static final int MIN_LINES_WITH_INTERNAL_CONTENT = 2;
    private static final String UNKNOWN_CLASS_NAME = "<unknown>";

    private final int blankLineThreshold;

    public WhitespaceSplitMethodRecipe() {
        this(DEFAULT_BLANK_LINE_THRESHOLD);
    }

    public WhitespaceSplitMethodRecipe(final int blankLineThreshold) {
        this.blankLineThreshold = blankLineThreshold;
    }

    public record Row(String className, String methodName, int blankLineCount, int totalLines, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Whitespace-split method detection (G30/Ch3.1)";
    }

    @Override
    public String getDescription() {
        return "Detects methods that use blank lines to separate logical sections, "
                + "suggesting they have multiple responsibilities and should be split.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new WhitespaceSplitMethodScanner(acc, blankLineThreshold);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class WhitespaceSplitMethodScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;
        private final int blankLineThreshold;

        WhitespaceSplitMethodScanner(final Accumulator acc, final int blankLineThreshold) {
            this.acc = acc;
            this.blankLineThreshold = blankLineThreshold;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            if (visited.getBody() == null) {
                return visited;
            }
            final List<String> lines = visited.getBody().print(getCursor()).lines().toList();
            final int blankLineCount = countInternalBlankLines(lines);
            if (blankLineCount >= blankLineThreshold) {
                acc.rows.add(new Row(findEnclosingClassName(), visited.getSimpleName(), blankLineCount, lines.size(), -1));
            }
            return visited;
        }

        int countInternalBlankLines(final List<String> lines) {
            if (lines.size() <= MIN_LINES_WITH_INTERNAL_CONTENT) {
                return 0;
            }
            final int lastInternalIndex = lines.size() - 1;
            int count = 0;
            for (int i = 1; i < lastInternalIndex; i++) {
                if (lines.get(i).isBlank()) {
                    count++;
                }
            }
            return count;
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS_NAME;
        }
    }
}
