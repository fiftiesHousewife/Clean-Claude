package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects legacy Java APIs that have well-established modern replacements.
 * Each row carries the legacy type name and a one-line replacement hint so
 * the message in the report can be specific.
 *
 * <p>Includes {@code Class.newInstance()} which is a method call (not a
 * type reference) — handled separately via {@link MethodMatcher}.
 */
public class LegacyTypesRecipe extends ScanningRecipe<LegacyTypesRecipe.Accumulator> {

    public record Row(String className, String legacyType, String replacement) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private static final Map<String, String> LEGACY_TYPE_REPLACEMENTS = buildReplacements();

    private static Map<String, String> buildReplacements() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("java.io.File", "use java.nio.file.Path / Files");
        map.put("java.io.FileInputStream", "use Files.newInputStream(Path)");
        map.put("java.io.FileOutputStream", "use Files.newOutputStream(Path)");
        map.put("java.io.FileReader", "use Files.newBufferedReader(Path, charset)");
        map.put("java.io.FileWriter", "use Files.newBufferedWriter(Path, charset)");
        map.put("java.util.Date", "use java.time.Instant or java.time.LocalDate / LocalDateTime");
        map.put("java.util.Calendar", "use java.time.* (LocalDateTime, ZonedDateTime, OffsetDateTime)");
        map.put("java.text.SimpleDateFormat", "use java.time.format.DateTimeFormatter — thread-safe and immutable");
        map.put("java.util.Vector", "use java.util.ArrayList — Vector's synchronisation is rarely what you want");
        map.put("java.util.Hashtable", "use java.util.HashMap, or ConcurrentHashMap if concurrency is required");
        map.put("java.util.Stack", "use java.util.ArrayDeque — Stack extends Vector and is synchronised");
        map.put("java.util.Random", "use ThreadLocalRandom.current() for performance, or SplittableRandom for parallel streams");
        map.put("java.lang.StringBuffer", "use java.lang.StringBuilder unless you genuinely need synchronised access");
        return Map.copyOf(map);
    }

    private static final MethodMatcher CLASS_NEW_INSTANCE = new MethodMatcher("java.lang.Class newInstance()");

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Legacy Java APIs detection (G26 — Be Precise)";
    }

    @Override
    public String getDescription() {
        return "Detects legacy types (java.io.File, Date, Vector, Hashtable, Stack, Random, "
                + "StringBuffer, etc.) and Class.newInstance() — each has a modern replacement.";
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
            public J.VariableDeclarations visitVariableDeclarations(final J.VariableDeclarations multiVariable,
                    final ExecutionContext ctx) {
                final J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                checkType(v.getTypeExpression());
                return v;
            }

            @Override
            public J.NewClass visitNewClass(final J.NewClass newClass, final ExecutionContext ctx) {
                final J.NewClass nc = super.visitNewClass(newClass, ctx);
                if (!(nc.getType() instanceof JavaType.FullyQualified fq)) {
                    return nc;
                }
                if (isInsideMatchingVariableDeclaration(fq.getFullyQualifiedName())) {
                    return nc;
                }
                record(fq.getFullyQualifiedName(), fq.getClassName());
                return nc;
            }

            private boolean isInsideMatchingVariableDeclaration(final String fqn) {
                final J.VariableDeclarations vd = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (vd == null || vd.getTypeExpression() == null) {
                    return false;
                }
                final JavaType vdType = vd.getTypeExpression().getType();
                return vdType instanceof JavaType.FullyQualified vfq
                        && vfq.getFullyQualifiedName().equals(fqn);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (CLASS_NEW_INSTANCE.matches(m)) {
                    acc.rows.add(new Row(findEnclosingClassName(), "Class.newInstance()",
                            "use Class.getDeclaredConstructor().newInstance() — newInstance() is deprecated since Java 9"));
                }
                return m;
            }

            private void checkType(final org.openrewrite.java.tree.TypeTree typeExpr) {
                if (typeExpr == null) {
                    return;
                }
                final JavaType type = typeExpr.getType();
                if (type instanceof JavaType.FullyQualified fq) {
                    record(fq.getFullyQualifiedName(), fq.getClassName());
                }
            }

            private void record(final String fqn, final String simpleName) {
                final String replacement = LEGACY_TYPE_REPLACEMENTS.get(fqn);
                if (replacement == null) {
                    return;
                }
                acc.rows.add(new Row(findEnclosingClassName(), simpleName, replacement));
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
