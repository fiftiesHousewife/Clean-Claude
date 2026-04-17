package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class GuardClauseRecipe extends ScanningRecipe<GuardClauseRecipe.Accumulator> {

    private static final int MIN_GUARDS = 2;

    public record Row(String className, String methodName, int guardCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Excessive guard clause detection (G29/G30)";
    }

    @Override
    public String getDescription() {
        return "Detects methods with multiple if-return/if-continue guard clauses that should be simplified.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new GuardScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static final class GuardScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        GuardScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if (m.getBody() == null) {
                return m;
            }
            final int guardCount = countMatching(m.getBody().getStatements(), GuardScanner::isReturnOrThrowGuard);
            if (guardCount >= MIN_GUARDS) {
                acc.rows.add(new Row(enclosingClassName(), m.getSimpleName(), guardCount));
            }
            return m;
        }

        @Override
        public J.ForEachLoop visitForEachLoop(final J.ForEachLoop forLoop, final ExecutionContext ctx) {
            final J.ForEachLoop fl = super.visitForEachLoop(forLoop, ctx);
            if (!(fl.getBody() instanceof J.Block body)) {
                return fl;
            }
            final int guardCount = countMatching(body.getStatements(), GuardScanner::isContinueGuard);
            if (guardCount >= MIN_GUARDS) {
                acc.rows.add(new Row(enclosingClassName(), enclosingMethodName(), guardCount));
            }
            return fl;
        }

        private String enclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
        }

        private String enclosingMethodName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDecl != null ? methodDecl.getSimpleName() : "<loop>";
        }

        private static int countMatching(final List<Statement> statements, final Predicate<Statement> predicate) {
            return (int) statements.stream().filter(predicate).count();
        }

        private static boolean isReturnOrThrowGuard(final Statement stmt) {
            return isBareIf(stmt) && bodyIsReturnOrThrow(((J.If) stmt).getThenPart());
        }

        private static boolean isContinueGuard(final Statement stmt) {
            return isBareIf(stmt) && bodyIsContinue(((J.If) stmt).getThenPart());
        }

        private static boolean isBareIf(final Statement stmt) {
            return stmt instanceof J.If ifStmt && ifStmt.getElsePart() == null;
        }

        private static boolean bodyIsReturnOrThrow(final Statement body) {
            return isReturnOrThrow(body)
                    || (body instanceof J.Block block && singleStatementMatches(block, GuardScanner::isReturnOrThrow));
        }

        private static boolean bodyIsContinue(final Statement body) {
            return body instanceof J.Continue
                    || (body instanceof J.Block block && singleStatementMatches(block, s -> s instanceof J.Continue));
        }

        private static boolean isReturnOrThrow(final Statement s) {
            return s instanceof J.Return || s instanceof J.Throw;
        }

        private static boolean singleStatementMatches(final J.Block block, final Predicate<Statement> predicate) {
            return block.getStatements().size() == 1 && predicate.test(block.getStatements().getFirst());
        }
    }
}
