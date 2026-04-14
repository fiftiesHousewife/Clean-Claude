package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CommentedCodeRecipe extends ScanningRecipe<CommentedCodeRecipe.Accumulator> {

    private static final int SCORE_THRESHOLD = 2;
    private static final Set<String> CODE_INDICATORS = Set.of(";", "{", "}", "return ", "if (", "for (", "while (");

    public record Row(String sourceFile, int lineNumber, String commentPreview) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Commented-out code detection (C5)";
    }

    @Override
    public String getDescription() {
        return "Detects comments that appear to contain code based on heuristic scoring.";
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
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                final J.CompilationUnit result = super.visitCompilationUnit(cu, ctx);
                final String sourcePath = cu.getSourcePath().toString();

                cu.getComments().forEach(comment -> checkComment(acc, comment, sourcePath));
                return result;
            }

            private void checkComment(Accumulator acc, Comment comment, String sourcePath) {
                if (!(comment instanceof TextComment textComment)) {
                    return;
                }

                final String text = textComment.getText();
                final int score = codeScore(text);
                if (score >= SCORE_THRESHOLD) {
                    final String preview = text.substring(0, Math.min(60, text.length())).trim();
                    acc.rows.add(new Row(sourcePath, -1, preview));
                }
            }

            private int codeScore(String text) {
                int score = 0;
                for (final String indicator : CODE_INDICATORS) {
                    if (text.contains(indicator)) {
                        score++;
                    }
                }
                return score;
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
