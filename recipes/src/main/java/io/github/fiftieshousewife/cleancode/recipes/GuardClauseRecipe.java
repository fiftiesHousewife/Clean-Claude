package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

public class GuardClauseRecipe extends ScanningRecipe<GuardClauseRecipe.Accumulator> {

    private static final int MIN_GUARDS = 2;

    public record Row(String className, String methodName, int guardCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Excessive guard clause detection (G30)";
    }

    @Override
    public String getDescription() {
        return "Detects methods with many guard clauses — multiple entry conditions usually mean the method "
                + "does several things and should be split. Guards themselves are encouraged by Clean Code; "
                + "the smell is the count, not the form.";
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

                final int guardCount = countGuardClauses(m.getBody().getStatements());
                if (guardCount >= MIN_GUARDS) {
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    acc.rows.add(new Row(className, m.getSimpleName(), guardCount));
                }

                return m;
            }

            @Override
            public J.ForEachLoop visitForEachLoop(final J.ForEachLoop forLoop, final ExecutionContext ctx) {
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
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    /**
     * Counts the number of <em>distinct exit shapes</em> among the leading
     * guard clauses, not the raw guard count. Three consecutive
     * {@code if (!precondition) return Optional.empty();} guards have the
     * same exit shape ({@code return Optional.empty();}) and form ONE
     * composite precondition — the method "rejects bad input" once. The
     * G30 smell is doing several different things; same-shape guards are
     * one thing said with multiple checks, not several things.
     *
     * <p>If guards return mixed shapes (some {@code throw}, some
     * {@code return -1}, some {@code return null}), they really are
     * separate behaviours and the rule fires.
     */
    private static int countGuardClauses(final List<Statement> statements) {
        return countDistinctGuardShapes(statements, GuardClauseRecipe::isGuardClause,
                GuardClauseRecipe::guardExitBodyShape);
    }

    private static int countContinueGuards(final List<Statement> statements) {
        return countDistinctGuardShapes(statements, GuardClauseRecipe::isContinueGuard,
                stmt -> "continue");
    }

    private static int countDistinctGuardShapes(
            final List<Statement> statements,
            final java.util.function.Predicate<Statement> isGuard,
            final java.util.function.Function<Statement, String> shapeOf) {
        final Set<String> shapes = new HashSet<>();
        for (final Statement stmt : statements) {
            if (isGuard.test(stmt)) {
                shapes.add(shapeOf.apply(stmt));
            }
        }
        return shapes.size();
    }

    private static String guardExitBodyShape(final Statement stmt) {
        Statement body = ((J.If) stmt).getThenPart();
        if (body instanceof J.Block block && block.getStatements().size() == 1) {
            body = block.getStatements().getFirst();
        }
        return shapeOf(body);
    }

    /**
     * Cursor-free structural hash of a J node — used to decide whether two
     * guard clauses have the "same exit shape." Walks the LST and emits a
     * canonical string. {@code Tree#print(Cursor)} would have been more
     * faithful but requires a cursor that climbs to a SourceFile, which we
     * don't have outside the visitor's own traversal.
     */
    private static String shapeOf(final Object node) {
        if (node == null) {
            return "null";
        }
        if (node instanceof J.Return r) {
            return "return:" + shapeOf(r.getExpression());
        }
        if (node instanceof J.Throw t) {
            return "throw:" + shapeOf(t.getException());
        }
        if (node instanceof J.Continue) {
            return "continue";
        }
        if (node instanceof J.Break) {
            return "break";
        }
        if (node instanceof J.NewClass nc) {
            return "new:" + (nc.getClazz() == null ? "?" : nc.getClazz().toString());
        }
        if (node instanceof J.MethodInvocation mi) {
            return "call:" + (mi.getSelect() == null ? "" : shapeOf(mi.getSelect()))
                    + ":" + mi.getSimpleName();
        }
        if (node instanceof J.Literal lit) {
            return "lit:" + lit.getValue();
        }
        if (node instanceof J.Identifier id) {
            return "id:" + id.getSimpleName();
        }
        if (node instanceof J.FieldAccess fa) {
            return "field:" + fa.getSimpleName();
        }
        return node.getClass().getSimpleName();
    }

    private static boolean isGuardClause(final Statement stmt) {
        if (!(stmt instanceof J.If ifStmt)) {
            return false;
        }
        if (ifStmt.getElsePart() != null) {
            return false;
        }
        return bodyIsReturnOrThrow(ifStmt.getThenPart());
    }

    private static boolean isContinueGuard(final Statement stmt) {
        if (!(stmt instanceof J.If ifStmt)) {
            return false;
        }
        if (ifStmt.getElsePart() != null) {
            return false;
        }
        return bodyIsContinue(ifStmt.getThenPart());
    }

    private static boolean bodyIsReturnOrThrow(final Statement body) {
        if (body instanceof J.Return || body instanceof J.Throw) {
            return true;
        }
        if (body instanceof J.Block block && block.getStatements().size() == 1) {
            final Statement inner = block.getStatements().getFirst();
            return inner instanceof J.Return || inner instanceof J.Throw;
        }
        return false;
    }

    private static boolean bodyIsContinue(final Statement body) {
        if (body instanceof J.Continue) {
            return true;
        }
        if (body instanceof J.Block block && block.getStatements().size() == 1) {
            return block.getStatements().getFirst() instanceof J.Continue;
        }
        return false;
    }
}
