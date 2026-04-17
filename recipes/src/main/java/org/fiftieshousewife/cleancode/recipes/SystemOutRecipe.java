package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemOutRecipe extends ScanningRecipe<SystemOutRecipe.Accumulator> {

    private static final String PRINT_STACK_TRACE = "printStackTrace";
    private static final String SYSTEM = "System";
    private static final String OUT = "out";
    private static final String ERR = "err";
    private static final String UNKNOWN_CLASS = "<unknown>";

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
        return new SystemOutScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class SystemOutScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        SystemOutScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            recordIfPrintStackTrace(m);
            recordIfSystemStreamCall(m);
            return m;
        }

        private void recordIfPrintStackTrace(final J.MethodInvocation m) {
            if (PRINT_STACK_TRACE.equals(m.getSimpleName())) {
                acc.rows.add(new Row(findEnclosingClassName(), PRINT_STACK_TRACE));
            }
        }

        private void recordIfSystemStreamCall(final J.MethodInvocation m) {
            if (!(m.getSelect() instanceof J.FieldAccess fieldAccess)) {
                return;
            }
            if (!isSystemStreamAccess(fieldAccess)) {
                return;
            }
            final String fieldName = fieldAccess.getSimpleName();
            acc.rows.add(new Row(findEnclosingClassName(), SYSTEM + "." + fieldName + "." + m.getSimpleName()));
        }

        private boolean isSystemStreamAccess(final J.FieldAccess fieldAccess) {
            final String fieldName = fieldAccess.getSimpleName();
            if (!OUT.equals(fieldName) && !ERR.equals(fieldName)) {
                return false;
            }
            return fieldAccess.getTarget() instanceof J.Identifier ident && SYSTEM.equals(ident.getSimpleName());
        }

        private String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS;
        }
    }
}
