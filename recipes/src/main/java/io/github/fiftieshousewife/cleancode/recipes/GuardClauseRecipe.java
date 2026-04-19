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
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (m.getBody() == null) {
                    return m;
                }

                final int guardCount = countGuardClauses(m.getBody().getStatements());
                if (guardCount >= MIN_GUARDS) {
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    acc.rows.add(new Row(className, m.getSimpleName(), guardCount));
                }

                return m;
            }

            @Override
            public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
                final J.ForEachLoop fl = super.visitForEachLoop(forLoop, ctx);
                if (!(fl.getBody() instanceof J.Block body)) {
                    return fl;
                }

                final int guardCount = countContinueGuards(body.getStatements());
                if (guardCount >= MIN_GUARDS) {
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    final String methodName = methodDecl != null ? methodDecl.getSimpleName() : "<loop>";
                    acc.rows.add(new Row(className, methodName, guardCount));
                }

                return fl;
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

    private static int countGuardClauses(List<Statement> statements) {
        int count = 0;
        for (final Statement stmt : statements) {
            if (isGuardClause(stmt)) {
                count++;
            }
        }
        return count;
    }

    private static int countContinueGuards(List<Statement> statements) {
        int count = 0;
        for (final Statement stmt : statements) {
            if (isContinueGuard(stmt)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isGuardClause(Statement stmt) {
        if (!(stmt instanceof J.If ifStmt)) {
            return false;
        }
        if (ifStmt.getElsePart() != null) {
            return false;
        }
        return bodyIsReturnOrThrow(ifStmt.getThenPart());
    }

    private static boolean isContinueGuard(Statement stmt) {
        if (!(stmt instanceof J.If ifStmt)) {
            return false;
        }
        if (ifStmt.getElsePart() != null) {
            return false;
        }
        return bodyIsContinue(ifStmt.getThenPart());
    }

    private static boolean bodyIsReturnOrThrow(Statement body) {
        if (body instanceof J.Return || body instanceof J.Throw) {
            return true;
        }
        if (body instanceof J.Block block && block.getStatements().size() == 1) {
            final Statement inner = block.getStatements().getFirst();
            return inner instanceof J.Return || inner instanceof J.Throw;
        }
        return false;
    }

    private static boolean bodyIsContinue(Statement body) {
        if (body instanceof J.Continue) {
            return true;
        }
        if (body instanceof J.Block block && block.getStatements().size() == 1) {
            return block.getStatements().getFirst() instanceof J.Continue;
        }
        return false;
    }
}
