package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class EmbeddedLanguageRecipe extends ScanningRecipe<EmbeddedLanguageRecipe.Accumulator> {

    private static final int MIN_STRING_LENGTH = 30;

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "<(?:html|body|div|span|p|h[1-6]|table|tr|td|th|ul|ol|li|a |form|input|head|style|script|meta|link )[\\s>]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\\b.*\\b(?:FROM|INTO|SET|TABLE|WHERE)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CSS_PATTERN = Pattern.compile(
            "\\{[^}]*(?:font-family|color|background|margin|padding|display|border|width|height)\\s*:");

    public record Row(String className, String methodName, String language) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Embedded language detection";
    }

    @Override
    public String getDescription() {
        return "Detects Java files with embedded HTML, SQL, or CSS in string literals.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            private final java.util.Set<String> seen = new java.util.HashSet<>();

            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                final J.Literal lit = super.visitLiteral(literal, ctx);
                if (!(lit.getValue() instanceof String text)) {
                    return lit;
                }
                if (text.length() < MIN_STRING_LENGTH) {
                    return lit;
                }

                final String language = detectLanguage(text);
                if (language != null) {
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    final String methodName = methodDecl != null ? methodDecl.getSimpleName() : "<field>";
                    final String key = className + "." + methodName + "." + language;
                    if (seen.add(key)) {
                        acc.rows.add(new Row(className, methodName, language));
                    }
                }
                return lit;
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

    static String detectLanguage(String text) {
        if (HTML_PATTERN.matcher(text).find()) {
            return "html";
        }
        if (SQL_PATTERN.matcher(text).find()) {
            return "sql";
        }
        if (CSS_PATTERN.matcher(text).find()) {
            return "css";
        }
        return null;
    }
}
