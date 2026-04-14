package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MagicStringRecipe extends ScanningRecipe<MagicStringRecipe.Accumulator> {

    private static final int MIN_LENGTH = 3;
    private static final int DEFAULT_MIN_OCCURRENCES = 2;

    private final int minOccurrences;

    public MagicStringRecipe() {
        this(DEFAULT_MIN_OCCURRENCES);
    }

    public MagicStringRecipe(final int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }
    private static final Set<String> IGNORED_VALUES = Set.of(
            "UTF-8", "application/json", "text/plain", "text/html",
            "Content-Type", "Accept", "Authorization",
            "<unknown>"
    );

    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    public record Row(String className, String value, int count, int lineNumber) {}

    record Occurrence(String value, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Magic string detection (G25)";
    }

    @Override
    public String getDescription() {
        return "Detects string literals that appear more than once in the same class and should be extracted to named constants.";
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
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                final String className = c.getSimpleName();
                final List<Occurrence> occurrences = new ArrayList<>();

                new JavaIsoVisitor<List<Occurrence>>() {
                    @Override
                    public J.Literal visitLiteral(J.Literal literal, List<Occurrence> collected) {
                        final J.Literal lit = super.visitLiteral(literal, collected);
                        if (isInsideAnnotation() || isInsideMapEntry()) {
                            return lit;
                        }
                        if (lit.getValue() instanceof String stringValue && isCandidate(stringValue)) {
                            final int line = lit.getMarkers().getMarkers().isEmpty()
                                    ? -1 : -1;
                            collected.add(new Occurrence(stringValue, -1));
                        }
                        return lit;
                    }

                    private boolean isInsideAnnotation() {
                        return getCursor().firstEnclosing(J.Annotation.class) != null;
                    }

                    private boolean isInsideMapEntry() {
                        final J.MethodInvocation enclosing = getCursor().firstEnclosing(J.MethodInvocation.class);
                        return enclosing != null && "entry".equals(enclosing.getSimpleName());
                    }
                }.visit(c.getBody(), occurrences);

                final Map<String, List<Occurrence>> grouped = new HashMap<>();
                occurrences.forEach(occ -> grouped.computeIfAbsent(occ.value(), k -> new ArrayList<>()).add(occ));

                grouped.forEach((value, occs) -> {
                    if (occs.size() >= minOccurrences) {
                        final int firstLine = occs.getFirst().lineNumber();
                        acc.rows.add(new Row(className, value, occs.size(), firstLine));
                    }
                });

                return c;
            }
        };
    }

    static boolean isCandidate(final String value) {
        if (value.length() < MIN_LENGTH) {
            return false;
        }
        if (IGNORED_VALUES.contains(value)) {
            return false;
        }
        if (value.isBlank()) {
            return false;
        }
        if (value.contains("<") || value.contains(">")) {
            return false;
        }
        if (value.contains("/") || value.contains(".claude/")) {
            return false;
        }
        if (value.length() < 15 && TOOL_ID_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
