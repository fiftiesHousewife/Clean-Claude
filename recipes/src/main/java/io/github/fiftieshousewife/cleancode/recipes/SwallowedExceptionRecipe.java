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

public class SwallowedExceptionRecipe extends ScanningRecipe<SwallowedExceptionRecipe.Accumulator> {

    public record Row(String className, String methodName, String exceptionType) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Swallowed exception detection (G4)";
    }

    @Override
    public String getDescription() {
        return "Detects catch blocks with empty bodies — exceptions silently swallowed.";
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
            public J.Try.Catch visitCatch(J.Try.Catch catchClause, ExecutionContext ctx) {
                final J.Try.Catch c = super.visitCatch(catchClause, ctx);
                final J.Block body = c.getBody();

                if (body.getStatements().isEmpty()) {
                    final J.VariableDeclarations param = c.getParameter().getTree();
                    final String typeName = param.getTypeExpression() != null
                            ? param.getTypeExpression().toString().trim() : "Exception";
                    final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                    final String methodName = methodDecl != null ? methodDecl.getSimpleName() : "<init>";
                    acc.rows.add(new Row(className, methodName, typeName));
                }

                return c;
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
