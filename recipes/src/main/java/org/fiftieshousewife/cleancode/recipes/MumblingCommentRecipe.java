package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MumblingCommentRecipe extends ScanningRecipe<MumblingCommentRecipe.Accumulator> {

    static final int MIN_COMMENT_LENGTH = 3;
    static final Pattern WHITESPACE = Pattern.compile("\\s+");

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
        return "Detects comments that restate the method name, parameter names, or the code that follows them.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new MumblingCommentScanner(acc);
    }

    static String normalise(String text) {
        final String alphaNumeric = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", " ");
        return WHITESPACE.matcher(alphaNumeric).replaceAll(" ").trim();
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
