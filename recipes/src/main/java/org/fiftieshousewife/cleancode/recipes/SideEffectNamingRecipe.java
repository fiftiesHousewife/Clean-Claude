package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SideEffectNamingRecipe extends ScanningRecipe<SideEffectNamingRecipe.Accumulator> {

    private static final Set<String> QUERY_PREFIXES = Set.of("get", "find", "is", "has", "check");

    public record Row(String className, String methodName, String sideEffect, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Side-effect naming detection (N7)";
    }

    @Override
    public String getDescription() {
        return "Detects methods whose name suggests a pure query but whose body modifies state via assignments.";
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

                if (hasOverrideAnnotation(m)) {
                    return m;
                }

                if (!hasQueryPrefix(m.getSimpleName())) {
                    return m;
                }

                if (m.getBody() == null) {
                    return m;
                }

                final String sideEffect = findSideEffect(m.getBody());
                if (sideEffect != null) {
                    acc.rows.add(new Row(
                            findEnclosingClassName(),
                            m.getSimpleName(),
                            sideEffect,
                            -1
                    ));
                }

                return m;
            }

            private boolean hasOverrideAnnotation(J.MethodDeclaration method) {
                return method.getLeadingAnnotations().stream()
                        .anyMatch(ann -> "Override".equals(ann.getSimpleName()));
            }

            private boolean hasQueryPrefix(String name) {
                for (final String prefix : QUERY_PREFIXES) {
                    if (name.startsWith(prefix) && name.length() > prefix.length()
                            && Character.isUpperCase(name.charAt(prefix.length()))) {
                        return true;
                    }
                }
                return false;
            }

            private String findSideEffect(J.Block body) {
                for (final var statement : body.getStatements()) {
                    if (statement instanceof J.Assignment assignment) {
                        return "assigns to " + assignment.getVariable().printTrimmed(getCursor());
                    }
                    if (statement instanceof J.AssignmentOperation assignOp) {
                        return "assigns to " + assignOp.getVariable().printTrimmed(getCursor());
                    }
                }
                return null;
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
