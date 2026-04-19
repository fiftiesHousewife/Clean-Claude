package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemOutRecipe extends ScanningRecipe<SystemOutRecipe.Accumulator> {

    public record Row(String className, String call) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "System.out/err detection (G4)";
    }

    @Override
    public String getDescription() {
        return "Detects System.out.println, System.err.println, and e.printStackTrace() in production code.";
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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                final String methodName = m.getSimpleName();

                if ("printStackTrace".equals(methodName)) {
                    acc.rows.add(new Row(findEnclosingClassName(), "printStackTrace"));
                    return m;
                }

                if (m.getSelect() instanceof J.FieldAccess fieldAccess) {
                    final String fieldName = fieldAccess.getSimpleName();
                    if (("out".equals(fieldName) || "err".equals(fieldName))
                            && fieldAccess.getTarget() instanceof J.Identifier ident
                            && "System".equals(ident.getSimpleName())) {
                        acc.rows.add(new Row(findEnclosingClassName(),
                                "System." + fieldName + "." + methodName));
                    }
                }

                return m;
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
