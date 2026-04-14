package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UncheckedCastRecipe extends ScanningRecipe<UncheckedCastRecipe.Accumulator> {

    public record Row(String className, String memberName, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Unchecked cast detection";
    }

    @Override
    public String getDescription() {
        return "Detects @SuppressWarnings(\"unchecked\") annotations that indicate type system workarounds.";
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
                final J.Annotation ann = super.visitAnnotation(annotation, ctx);

                if (!isSuppressWarnings(ann)) {
                    return ann;
                }

                if (!containsUnchecked(ann)) {
                    return ann;
                }

                final String className = findEnclosingClassName();
                final String memberName = findEnclosingMemberName();
                acc.rows.add(new Row(className, memberName, -1));

                return ann;
            }

            private boolean isSuppressWarnings(J.Annotation annotation) {
                final J.Identifier annotationType = annotation.getAnnotationType() instanceof J.Identifier id
                        ? id : null;
                return annotationType != null && "SuppressWarnings".equals(annotationType.getSimpleName());
            }

            private boolean containsUnchecked(J.Annotation annotation) {
                if (annotation.getArguments() == null) {
                    return false;
                }
                for (org.openrewrite.java.tree.Expression arg : annotation.getArguments()) {
                    if (arg instanceof J.Literal literal && literal.getValue() instanceof String value) {
                        if (value.contains("unchecked")) {
                            return true;
                        }
                    }
                    if (arg instanceof J.NewArray newArray && newArray.getInitializer() != null) {
                        for (org.openrewrite.java.tree.Expression element : newArray.getInitializer()) {
                            if (element instanceof J.Literal literal
                                    && literal.getValue() instanceof String value
                                    && value.contains("unchecked")) {
                                return true;
                            }
                        }
                    }
                    if (arg instanceof J.Assignment assignment) {
                        final org.openrewrite.java.tree.Expression value = assignment.getAssignment();
                        if (value instanceof J.Literal literal
                                && literal.getValue() instanceof String strVal
                                && strVal.contains("unchecked")) {
                            return true;
                        }
                        if (value instanceof J.NewArray newArray && newArray.getInitializer() != null) {
                            for (org.openrewrite.java.tree.Expression element : newArray.getInitializer()) {
                                if (element instanceof J.Literal literal
                                        && literal.getValue() instanceof String strVal
                                        && strVal.contains("unchecked")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }

            private String findEnclosingMemberName() {
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (methodDecl != null) {
                    return methodDecl.getSimpleName();
                }
                final J.VariableDeclarations varDecls = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (varDecls != null && !varDecls.getVariables().isEmpty()) {
                    return varDecls.getVariables().getFirst().getSimpleName();
                }
                return "<class-level>";
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
