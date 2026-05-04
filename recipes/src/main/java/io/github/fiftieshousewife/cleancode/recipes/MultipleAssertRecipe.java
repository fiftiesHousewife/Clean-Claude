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
import java.util.Set;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

public class MultipleAssertRecipe extends ScanningRecipe<MultipleAssertRecipe.Accumulator> {

    private static final int DEFAULT_THRESHOLD = 2;
    private static final Set<String> ASSERT_PREFIXES = Set.of(
            "assertThat", "assertEquals", "assertTrue", "assertFalse",
            "assertNull", "assertNotNull", "assertThrows", "assertSame");

    private final int threshold;

    public MultipleAssertRecipe(final int threshold) {
        this.threshold = threshold;
    }

    public MultipleAssertRecipe() {
        this(DEFAULT_THRESHOLD);
    }

    public record Row(String className, String methodName, int assertCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Multiple unwrapped assertions detection (T1)";
    }

    @Override
    public String getDescription() {
        return "Detects test methods with consecutive assertions not wrapped in assertAll.";
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
                if (!isTestMethod(m) || m.getBody() == null) {
                    return m;
                }
                final int consecutive = longestAssertRun(m.getBody().getStatements());
                if (consecutive >= threshold) {
                    final String className = findEnclosingClassName();
                    acc.rows.add(new Row(className, m.getSimpleName(), consecutive));
                }
                return m;
            }

            private boolean isTestMethod(final J.MethodDeclaration method) {
                return method.getLeadingAnnotations().stream()
                        .anyMatch(ann -> "Test".equals(ann.getSimpleName()));
            }

            private int longestAssertRun(final List<Statement> statements) {
                int maxRun = 0;
                int currentRun = 0;
                for (final Statement stmt : statements) {
                    if (isAssertCall(stmt)) {
                        currentRun++;
                        maxRun = Math.max(maxRun, currentRun);
                    } else {
                        currentRun = 0;
                    }
                }
                return maxRun;
            }

            private boolean isAssertCall(final Statement stmt) {
                if (!(stmt instanceof J.MethodInvocation invocation)) {
                    return false;
                }
                return isAssertInvocation(invocation);
            }

            private boolean isAssertInvocation(final J.MethodInvocation invocation) {
                if (ASSERT_PREFIXES.stream().anyMatch(p -> invocation.getSimpleName().startsWith(p))) {
                    return true;
                }
                if (invocation.getSelect() instanceof J.MethodInvocation parent) {
                    return isAssertInvocation(parent);
                }
                return false;
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
