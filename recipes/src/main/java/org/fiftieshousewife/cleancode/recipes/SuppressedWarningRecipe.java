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

public class SuppressedWarningRecipe extends ScanningRecipe<SuppressedWarningRecipe.Accumulator> {

    private static final Set<String> SAFETY_WARNINGS = Set.of(
            "unchecked", "rawtypes", "deprecation", "serial");
    private static final String SUPPRESS_WARNINGS = "SuppressWarnings";
    private static final String UNKNOWN_CLASS = "<unknown>";
    private static final String CLASS_LEVEL_MEMBER = "<class>";

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
        return new SuppressedWarningScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    static String argumentText(final J.Annotation annotation) {
        return annotation.getArguments() != null ? annotation.getArguments().toString() : "";
    }

    static final class SuppressedWarningScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        SuppressedWarningScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.Annotation visitAnnotation(final J.Annotation annotation, final ExecutionContext ctx) {
            final J.Annotation visited = super.visitAnnotation(annotation, ctx);
            if (!SUPPRESS_WARNINGS.equals(visited.getSimpleName())) {
                return visited;
            }
            recordMatchingWarnings(argumentText(visited));
            return visited;
        }

        void recordMatchingWarnings(final String argText) {
            SAFETY_WARNINGS.stream()
                    .filter(argText::contains)
                    .forEach(this::recordWarning);
        }

        void recordWarning(final String warning) {
            acc.rows.add(new Row(findEnclosingClassName(), findEnclosingMethodName(), warning));
        }

        String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS;
        }

        String findEnclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : CLASS_LEVEL_MEMBER;
        }
    }
}
