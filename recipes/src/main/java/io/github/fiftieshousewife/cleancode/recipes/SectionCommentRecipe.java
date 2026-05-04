package io.github.fiftieshousewife.cleancode.recipes;

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
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

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

    public SectionCommentRecipe(final int threshold) {
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
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (BoilerplateMethodSkip.isContractMethod(m)) {
                    return m;
                }
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

            private int countSectionComments(final List<Statement> statements) {
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

            private boolean isSectionComment(final String text) {
                final String trimmed = text.trim();
                if (trimmed.isEmpty()) {
                    return false;
                }
                final String lower = trimmed.toLowerCase(Locale.ROOT);
                if (ANNOTATION_PREFIXES.stream().anyMatch(lower::startsWith)) {
                    return false;
                }
                // Per Clean Code G34, a section comment is a short
                // banner — `// initialisation`, `// processing`,
                // `// cleanup`, or visual separators like `===`. Long
                // explanatory comments are documentation, not section
                // markers; ignore them.
                return looksLikeASectionBanner(trimmed);
            }

            private boolean looksLikeASectionBanner(final String trimmed) {
                if (trimmed.startsWith("=") || trimmed.startsWith("-")
                        || trimmed.startsWith("*")) {
                    return true;
                }
                final String[] words = trimmed.split("\\s+");
                if (words.length > 5) {
                    return false;
                }
                // No sentence-ending punctuation — banners are labels,
                // not sentences. Allow a single trailing colon.
                final String stripTrailingColon = trimmed.endsWith(":")
                        ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                if (stripTrailingColon.matches(".*[.,?!;].*")) {
                    return false;
                }
                return true;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
