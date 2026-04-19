package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemporalCouplingRecipe extends ScanningRecipe<TemporalCouplingRecipe.Accumulator> {

    private final int minConsecutiveCalls;

    public record Row(String className, String methodName, int callCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    public TemporalCouplingRecipe(int minConsecutiveCalls) {
        this.minConsecutiveCalls = minConsecutiveCalls;
    }

    @Override
    public String getDisplayName() {
        return "Hidden temporal coupling detection (G31)";
    }

    @Override
    public String getDescription() {
        return "Detects sequences of %d+ void method calls with no data dependency between them."
                .formatted(minConsecutiveCalls);
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
                if (m.getBody() == null) {
                    return m;
                }

                final int maxRun = longestVoidCallRun(m.getBody().getStatements());
                if (maxRun >= minConsecutiveCalls) {
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    acc.rows.add(new Row(className, m.getSimpleName(), maxRun));
                }

                return m;
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

    private static int longestVoidCallRun(List<Statement> statements) {
        int maxRun = 0;
        int currentRun = 0;

        for (final Statement stmt : statements) {
            if (isStandaloneMethodCall(stmt)) {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            } else {
                currentRun = 0;
            }
        }

        return maxRun;
    }

    private static boolean isStandaloneMethodCall(Statement stmt) {
        return stmt instanceof J.MethodInvocation;
    }
}
