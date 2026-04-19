package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DisabledTestRecipe extends ScanningRecipe<DisabledTestRecipe.Accumulator> {

    private static final Set<String> DISABLED_ANNOTATIONS = Set.of("Disabled", "Ignore");
    private static final Set<String> PLACEHOLDER_REASONS = Set.of("", "TODO", "todo", "FIXME", "fixme");

    public record Row(String className, String methodName, String annotation, boolean hasReason, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Disabled test detection (T3/T4)";
    }

    @Override
    public String getDescription() {
        return "Detects @Disabled/@Ignore annotations without meaningful reason strings.";
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
                m.getLeadingAnnotations().forEach(ann -> checkAnnotation(acc, ann, m.getSimpleName()));
                return m;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                c.getLeadingAnnotations().forEach(ann -> checkAnnotation(acc, ann, "<class>"));
                return c;
            }

            private void checkAnnotation(Accumulator acc, J.Annotation ann, String methodName) {
                final String annName = ann.getSimpleName();
                if (!DISABLED_ANNOTATIONS.contains(annName)) {
                    return;
                }

                final boolean hasReason = hasMeaningfulReason(ann);
                if (!hasReason) {
                    final String className = findEnclosingClassName();
                    acc.rows.add(new Row(className, methodName, annName, false, -1));
                }
            }

            private boolean hasMeaningfulReason(J.Annotation ann) {
                if (ann.getArguments() == null || ann.getArguments().isEmpty()) {
                    return false;
                }

                return ann.getArguments().stream()
                        .filter(arg -> arg instanceof J.Literal)
                        .map(arg -> ((J.Literal) arg).getValue())
                        .filter(val -> val instanceof String)
                        .map(val -> ((String) val).trim())
                        .anyMatch(val -> !PLACEHOLDER_REASONS.contains(val));
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
