package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe.Row;
import static org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe.WHITESPACE;
import static org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe.camelToWords;
import static org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe.normalise;

final class MumblingCommentScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final int MAX_PREVIEW_LENGTH = 60;
    private static final int UNKNOWN_LINE = -1;
    private static final String UNKNOWN_CLASS = "<unknown>";

    private final MumblingCommentRecipe.Accumulator acc;

    MumblingCommentScanner(final MumblingCommentRecipe.Accumulator acc) {
        this.acc = acc;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
        final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
        final MumblingContext context = new MumblingContext(
                findEnclosingClassName(),
                visited.getSimpleName(),
                extractParamNames(visited));
        final List<String> lines = visited.print(getCursor()).lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            inspectLine(lines, i, context);
        }
        return visited;
    }

    private void inspectLine(final List<String> lines, final int index, final MumblingContext context) {
        final String commentText = LineCommentExtractor.extract(lines.get(index));
        if (commentText == null) {
            return;
        }
        final String normalised = normalise(commentText);
        final String nextLine = index + 1 < lines.size() ? lines.get(index + 1).trim() : "";
        if (isMumbling(normalised, context, nextLine)) {
            acc.rows.add(new Row(context.className(), context.methodName(), preview(commentText), UNKNOWN_LINE));
        }
    }

    private String preview(final String commentText) {
        return commentText.substring(0, Math.min(MAX_PREVIEW_LENGTH, commentText.length()));
    }

    boolean isMumbling(final String normalisedComment, final MumblingContext context, final String nextCodeLine) {
        if (restatesMethodName(normalisedComment, context.methodName())) {
            return true;
        }
        if (restatesAllParameters(normalisedComment, context.paramNames())) {
            return true;
        }
        return !nextCodeLine.isEmpty() && restatesNextLine(normalisedComment, nextCodeLine);
    }

    private boolean restatesMethodName(final String normalisedComment, final String methodName) {
        final String[] methodWords = WHITESPACE.split(normalise(camelToWords(methodName)));
        return CommentWordOverlap.allWordsPresent(normalisedComment, methodWords);
    }

    private boolean restatesAllParameters(final String normalisedComment, final Set<String> paramNames) {
        if (paramNames.isEmpty()) {
            return false;
        }
        final long matchCount = paramNames.stream()
                .filter(p -> normalisedComment.contains(normalise(p)))
                .count();
        return matchCount >= paramNames.size();
    }

    boolean restatesNextLine(final String normalisedComment, final String nextCodeLine) {
        final String[] commentWords = WHITESPACE.split(normalisedComment);
        final String[] codeWords = WHITESPACE.split(normalise(nextCodeLine));
        if (commentWords.length < MumblingThresholds.MIN_WORDS
                || codeWords.length < MumblingThresholds.MIN_WORDS) {
            return false;
        }
        final long significantCodeWords = CommentWordOverlap.countSignificantWords(codeWords);
        final long codeWordsInComment = CommentWordOverlap.countSignificantWordsFoundIn(codeWords, normalisedComment);
        return CommentWordOverlap.hasEnoughOverlap(significantCodeWords, codeWordsInComment);
    }

    private Set<String> extractParamNames(final J.MethodDeclaration method) {
        final Set<String> names = new HashSet<>();
        method.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .forEach(v -> v.getVariables().forEach(named -> names.add(named.getSimpleName())));
        return names;
    }

    private String findEnclosingClassName() {
        final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS;
    }

    record MumblingContext(String className, String methodName, Set<String> paramNames) {}
}
