package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SectionCommentRecipe extends ScanningRecipe<SectionCommentRecipe.Accumulator> {

    private final int threshold;
    private static final Set<String> ANNOTATION_PREFIXES = Set.of(
            "todo", "fixme", "hack", "xxx", "nopmd", "nosonar",
            "noinspection", "suppress", "@", "fall", "intentional",
            "cpd-off", "cpd-on", "region", "endregion");

    public record Row(String className, String methodName, int sectionCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    public SectionCommentRecipe(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getDisplayName() {
        return "Section comment detection (G34)";
    }

    @Override
    public String getDescription() {
        return "Detects methods using inline comments to separate code into sections that should be extracted.";
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

                final int sectionCount = countSectionComments(m.getBody().getStatements());
                if (sectionCount >= threshold) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            m.getSimpleName(),
                            sectionCount,
                            -1));
                }

                return m;
            }

            private int countSectionComments(List<Statement> statements) {
                int count = 0;
                for (final Statement stmt : statements) {
                    for (final Comment comment : stmt.getComments()) {
                        if (comment instanceof TextComment text && isSectionComment(text.getText())) {
                            count++;
                        }
                    }
                }
                return count;
            }

            private boolean isSectionComment(String text) {
                final String trimmed = text.trim();
                if (trimmed.isEmpty()) {
                    return false;
                }
                final String lower = trimmed.toLowerCase(Locale.ROOT);
                return ANNOTATION_PREFIXES.stream().noneMatch(lower::startsWith);
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
