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
import java.util.Locale;
import java.util.Set;

public class MumblingCommentRecipe extends ScanningRecipe<MumblingCommentRecipe.Accumulator> {

    public record Row(String className, String methodName, String commentPreview, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Mumbling comment detection (C3)";
    }

    @Override
    public String getDescription() {
        return "Detects comments that merely restate the method name, parameter names, or are trivially redundant.";
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
                final String methodName = m.getSimpleName();
                final Set<String> paramNames = extractParamNames(m);
                final String className = findEnclosingClassName();
                final String printed = m.print(getCursor());

                extractLineComments(printed).stream()
                        .filter(text -> isMumbling(normalise(text), methodName, paramNames))
                        .forEach(text -> acc.rows.add(new Row(className, methodName,
                                text.substring(0, Math.min(60, text.length())), -1)));

                return m;
            }

            private List<String> extractLineComments(String source) {
                final List<String> comments = new ArrayList<>();
                source.lines().forEach(line -> {
                    final int idx = line.indexOf("//");
                    if (idx >= 0) {
                        comments.add(line.substring(idx + 2).trim());
                    }
                });
                return comments;
            }

            private boolean isMumbling(String normalisedComment, String methodName, Set<String> paramNames) {
                final String[] methodWords = normalise(camelToWords(methodName)).split("\\s+");

                if (allWordsPresent(normalisedComment, methodWords)) {
                    return true;
                }

                if (!paramNames.isEmpty()) {
                    final long matchCount = paramNames.stream()
                            .filter(p -> normalisedComment.contains(normalise(p)))
                            .count();
                    return matchCount >= paramNames.size();
                }

                return false;
            }

            private boolean allWordsPresent(String text, String[] words) {
                if (words.length < 2) {
                    return false;
                }
                final String[] textWords = text.split("\\s+");
                for (final String word : words) {
                    if (word.length() < 2) {
                        continue;
                    }
                    final boolean found = java.util.Arrays.stream(textWords)
                            .anyMatch(tw -> tw.startsWith(word) || word.startsWith(tw));
                    if (!found) {
                        return false;
                    }
                }
                return true;
            }

            private Set<String> extractParamNames(J.MethodDeclaration method) {
                final var names = new java.util.HashSet<String>();
                method.getParameters().stream()
                        .filter(p -> p instanceof J.VariableDeclarations)
                        .map(p -> (J.VariableDeclarations) p)
                        .forEach(v -> v.getVariables().forEach(
                                var -> names.add(var.getSimpleName())));
                return names;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String camelToWords(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
