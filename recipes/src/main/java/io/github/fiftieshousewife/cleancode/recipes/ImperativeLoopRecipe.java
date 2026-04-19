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

public class ImperativeLoopRecipe extends ScanningRecipe<ImperativeLoopRecipe.Accumulator> {

    public record Row(String className, String methodName, String loopPattern, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Imperative loop detection";
    }

    @Override
    public String getDescription() {
        return "Detects for-loops that could be replaced with stream operations.";
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
            public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, ExecutionContext ctx) {
                final J.ForEachLoop loop = super.visitForEachLoop(forEachLoop, ctx);
                final Statement body = extractSingleStatement(loop.getBody());

                if (body == null) {
                    return loop;
                }

                final String pattern = classifyBody(body);
                if (pattern != null) {
                    final String className = findEnclosingClassName();
                    final String methodName = findEnclosingMethodName();
                    acc.rows.add(new Row(className, methodName, pattern, -1));
                }

                return loop;
            }

            @Override
            public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                final J.ForLoop loop = super.visitForLoop(forLoop, ctx);
                final Statement body = extractSingleStatement(loop.getBody());

                if (body == null) {
                    return loop;
                }

                final String pattern = classifyBody(body);
                if (pattern != null) {
                    final String className = findEnclosingClassName();
                    final String methodName = findEnclosingMethodName();
                    acc.rows.add(new Row(className, methodName, "for with " + pattern.replace("for-each with ", ""),
                            -1));
                }

                return loop;
            }

            private Statement extractSingleStatement(Statement body) {
                if (body instanceof J.Block block) {
                    final List<Statement> statements = block.getStatements();
                    if (statements.size() != 1) {
                        return null;
                    }
                    return statements.getFirst();
                }
                return body;
            }

            private String classifyBody(Statement statement) {
                if (containsFlowControl(statement)) {
                    return null;
                }

                if (isAddCall(statement)) {
                    return "for-each with add";
                }

                if (statement instanceof J.If ifStmt) {
                    final Statement thenBody = extractSingleStatement(ifStmt.getThenPart());
                    if (thenBody != null && isAddCall(thenBody) && ifStmt.getElsePart() == null) {
                        return "for-each with filter-add";
                    }
                }

                return null;
            }

            private boolean isAddCall(Statement statement) {
                if (statement instanceof J.MethodInvocation invocation) {
                    return "add".equals(invocation.getSimpleName());
                }
                return false;
            }

            private boolean containsFlowControl(Statement statement) {
                final boolean[] found = {false};
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Break visitBreak(J.Break breakStatement, Integer unused) {
                        found[0] = true;
                        return breakStatement;
                    }

                    @Override
                    public J.Continue visitContinue(J.Continue continueStatement, Integer unused) {
                        found[0] = true;
                        return continueStatement;
                    }

                    @Override
                    public J.Return visitReturn(J.Return returnStatement, Integer unused) {
                        found[0] = true;
                        return returnStatement;
                    }
                }.visit(statement, 0);
                return found[0];
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }

            private String findEnclosingMethodName() {
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return methodDecl != null ? methodDecl.getSimpleName() : "<unknown>";
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
