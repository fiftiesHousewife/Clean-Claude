package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

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
                m.getLeadingAnnotations().forEach(ann -> checkAnnotation(acc, ann, m.getSimpleName()));
                return m;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                c.getLeadingAnnotations().forEach(ann -> checkAnnotation(acc, ann, "<class>"));
                return c;
            }

            private void checkAnnotation(final Accumulator acc, final J.Annotation ann, final String methodName) {
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

            private boolean hasMeaningfulReason(final J.Annotation ann) {
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
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
