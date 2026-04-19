package io.github.fiftieshousewife.cleancode.recipes;

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
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (m.getBody() == null) {
                    return m;
                }

                final String body = m.getBody().print(getCursor());
                final List<String> lines = body.lines().toList();
                final int blankLineCount = countInternalBlankLines(lines);
                final int totalLines = lines.size();

                if (blankLineCount >= blankLineThreshold) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            m.getSimpleName(),
                            blankLineCount,
                            totalLines,
                            -1));
                }

                return m;
            }

            private int countInternalBlankLines(List<String> lines) {
                if (lines.size() <= 2) {
                    return 0;
                }

                int count = 0;
                for (int i = 1; i < lines.size() - 1; i++) {
                    if (lines.get(i).isBlank()) {
                        count++;
                    }
                }
                return count;
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
