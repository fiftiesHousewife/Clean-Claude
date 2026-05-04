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

    public TemporalCouplingRecipe(final int minConsecutiveCalls) {
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

            /**
             * Returns the largest count of <em>distinct</em> {@code (receiver,
             * methodName)} pairs in any contiguous run of standalone method
             * calls within the body. A run of identical calls — e.g. a
             * registration loop like {@code plugins.apply(X);
             * plugins.apply(Y);} — collapses to 1 distinct pair and does
             * not fire. Genuine temporal coupling like {@code setHost();
             * setPort(); connect();} has many distinct pairs.
             */
            private int longestVoidCallRun(final List<Statement> statements) {
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

            private String callKey(final J.MethodInvocation invocation) {
                final Expression select = invocation.getSelect();
                // Use printTrimmed for a stable source-form receiver
                // text. Expression.toString() includes AST internals so
                // two identical chains in the source produce different
                // strings, which would defeat the distinct-count.
                final String receiver = select == null ? "" : select.printTrimmed(getCursor());
                return receiver + "::" + invocation.getSimpleName();
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
