package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OutputArgumentRecipe extends ScanningRecipe<OutputArgumentRecipe.Accumulator> {

    private static final Set<String> MUTATING_METHODS = Set.of("add", "put", "set", "remove", "clear", "addAll", "putAll");

    public record Row(String className, String methodName, String paramName,
                      String paramType, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Output argument detection (F2)";
    }

    @Override
    public String getDescription() {
        return "Detects method parameters that are mutated within the method body.";
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
                final String className = findEnclosingClassName();
                final String methodBody = m.getBody() != null ? m.getBody().print(getCursor()) : "";

                m.getParameters().stream()
                        .filter(p -> p instanceof J.VariableDeclarations)
                        .map(p -> (J.VariableDeclarations) p)
                        .forEach(varDecl -> varDecl.getVariables().forEach(v -> {
                            final String paramName = v.getSimpleName();
                            if (isMutatedInBody(paramName, methodBody)) {
                                final String typeName = varDecl.getType() != null
                                        ? varDecl.getType().toString() : "unknown";
                                acc.rows.add(new Row(className, m.getSimpleName(),
                                        paramName, typeName, -1));
                            }
                        }));

                return m;
            }

            private boolean isMutatedInBody(String paramName, String body) {
                return MUTATING_METHODS.stream()
                        .anyMatch(method -> body.contains(paramName + "." + method + "("));
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
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
