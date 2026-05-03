package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

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

                if (BoilerplateMethodSkip.isContractMethod(m)) {
                    return m;
                }
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

    /**
     * Returns the largest count of <em>distinct</em> {@code (receiver,
     * methodName)} pairs found in any contiguous run of standalone method
     * calls. A run of identical calls — e.g. a registration loop like
     * {@code plugins.apply(X); plugins.apply(Y); plugins.apply(Z);} —
     * collapses to 1 distinct pair and so does not fire. Genuine temporal
     * coupling (e.g. {@code setHost(); setPort(); connect();}) has many
     * distinct pairs.
     */
    private static int longestVoidCallRun(List<Statement> statements) {
        int maxDistinct = 0;
        Set<String> currentRun = new HashSet<>();

        for (final Statement stmt : statements) {
            if (stmt instanceof J.MethodInvocation invocation) {
                currentRun.add(callKey(invocation));
                maxDistinct = Math.max(maxDistinct, currentRun.size());
            } else {
                currentRun = new HashSet<>();
            }
        }

        return maxDistinct;
    }

    private static String callKey(final J.MethodInvocation invocation) {
        final Expression select = invocation.getSelect();
        final String receiver = select == null ? "" : select.toString();
        return receiver + "::" + invocation.getSimpleName();
    }
}
