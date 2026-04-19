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

public class SuppressedWarningRecipe extends ScanningRecipe<SuppressedWarningRecipe.Accumulator> {

    private static final Set<String> SAFETY_WARNINGS = Set.of(
            "unchecked", "rawtypes", "deprecation", "serial");

    public record Row(String className, String methodName, String warningType) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "@SuppressWarnings safety override detection (G4)";
    }

    @Override
    public String getDescription() {
        return "Detects @SuppressWarnings for unchecked, rawtypes, deprecation — overriding compiler safeties.";
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
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                final J.Annotation a = super.visitAnnotation(annotation, ctx);

                if (!"SuppressWarnings".equals(a.getSimpleName())) {
                    return a;
                }

                final String argText = a.getArguments() != null
                        ? a.getArguments().toString() : "";

                SAFETY_WARNINGS.stream()
                        .filter(argText::contains)
                        .forEach(warning -> {
                            final J.ClassDeclaration classDecl = getCursor()
                                    .firstEnclosing(J.ClassDeclaration.class);
                            final J.MethodDeclaration methodDecl = getCursor()
                                    .firstEnclosing(J.MethodDeclaration.class);
                            final String className = classDecl != null
                                    ? classDecl.getSimpleName() : "<unknown>";
                            final String methodName = methodDecl != null
                                    ? methodDecl.getSimpleName() : "<class>";
                            acc.rows.add(new Row(className, methodName, warning));
                        });

                return a;
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
