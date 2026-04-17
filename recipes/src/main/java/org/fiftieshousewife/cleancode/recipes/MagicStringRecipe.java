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
    private static final int MAX_TOOL_ID_LENGTH = 15;
    private static final int DEFAULT_MIN_OCCURRENCES = 2;
    private static final int UNKNOWN_LINE = -1;

    // G35 skipped intentionally: these are intrinsic to the "magic string" heuristic itself —
    // moving them to config would require config to bootstrap this recipe, and callers expect
    // consistent exclusions across runs. The set is small and stable.
    private static final Set<String> IGNORED_VALUES = Set.of(
            "UTF-8", "application/json", "text/plain", "text/html",
            "Content-Type", "Accept", "Authorization",
            "<unknown>"
    );

    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    private final int minOccurrences;

    private Accumulator lastAccumulator;

    public MagicStringRecipe() {
        this(DEFAULT_MIN_OCCURRENCES);
    }

    public MagicStringRecipe(final int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    public record Row(String className, String value, int count, int lineNumber) {}

    record Occurrence(String value, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    @Override
    public String getDisplayName() {
        return "Magic string detection (G25)";
    }

    @Override
    public String getDescription() {
        return "Detects string literals that appear more than once in the same class "
                + "and should be extracted to named constants.";
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
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                final J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
                recordDuplicateLiterals(visited, acc);
                return visited;
            }
        };
    }

    private void recordDuplicateLiterals(final J.ClassDeclaration classDecl, final Accumulator acc) {
        final List<Occurrence> occurrences = collectCandidateLiterals(classDecl);
        final Map<String, List<Occurrence>> grouped = groupByValue(occurrences);
        final String className = classDecl.getSimpleName();
        grouped.forEach((value, occs) -> {
            if (occs.size() >= minOccurrences) {
                acc.rows.add(new Row(className, value, occs.size(), occs.getFirst().lineNumber()));
            }
        });
    }

    private static List<Occurrence> collectCandidateLiterals(final J.ClassDeclaration classDecl) {
        final LiteralCollector collector = new LiteralCollector();
        collector.visit(classDecl.getBody(), 0);
        return collector.occurrences();
    }

    private static Map<String, List<Occurrence>> groupByValue(final List<Occurrence> occurrences) {
        final Map<String, List<Occurrence>> grouped = new HashMap<>();
        occurrences.forEach(occ -> grouped.computeIfAbsent(occ.value(), k -> new ArrayList<>()).add(occ));
        return grouped;
    }

    private static final class LiteralCollector extends JavaIsoVisitor<Integer> {
        private final List<Occurrence> occurrences = new ArrayList<>();

        List<Occurrence> occurrences() {
            return occurrences;
        }

        @Override
        public J.Literal visitLiteral(final J.Literal literal, final Integer ignored) {
            final J.Literal lit = super.visitLiteral(literal, ignored);
            if (isInsideAnnotation() || isInsideMapEntry()) {
                return lit;
            }
            if (lit.getValue() instanceof String stringValue && isCandidate(stringValue)) {
                occurrences.add(new Occurrence(stringValue, UNKNOWN_LINE));
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
    }

    static boolean isCandidate(final String value) {
        return value.length() >= MIN_LENGTH
                && !value.isBlank()
                && !IGNORED_VALUES.contains(value)
                && !containsAngleBracket(value)
                && !containsPathSeparator(value)
                && !isShortToolId(value);
    }

    private static boolean containsAngleBracket(final String value) {
        return value.contains("<") || value.contains(">");
    }

    private static boolean containsPathSeparator(final String value) {
        return value.contains("/") || value.contains(".claude/");
    }

    private static boolean isShortToolId(final String value) {
        return value.length() < MAX_TOOL_ID_LENGTH && TOOL_ID_PATTERN.matcher(value).matches();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
