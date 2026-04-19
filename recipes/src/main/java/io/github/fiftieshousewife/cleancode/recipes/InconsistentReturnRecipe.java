package io.github.fiftieshousewife.cleancode.recipes;

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

public class InconsistentReturnRecipe extends ScanningRecipe<InconsistentReturnRecipe.Accumulator> {

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "List", "Set", "Map", "Collection");

    public record Row(String className, int returningMethods, int mutatingMethods) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Inconsistent collection return style detection (F2)";
    }

    @Override
    public String getDescription() {
        return "Detects classes where some methods return collections and others mutate collection parameters.";
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
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                int returningMethods = 0;
                int mutatingMethods = 0;

                for (final Statement stmt : c.getBody().getStatements()) {
                    if (!(stmt instanceof J.MethodDeclaration method)) {
                        continue;
                    }
                    if (returnsCollection(method)) {
                        returningMethods++;
                    }
                    if (mutatesCollectionParam(method)) {
                        mutatingMethods++;
                    }
                }

                if (returningMethods > 0 && mutatingMethods > 0) {
                    acc.rows.add(new Row(c.getSimpleName(), returningMethods, mutatingMethods));
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

    private static boolean returnsCollection(J.MethodDeclaration method) {
        if (method.getReturnTypeExpression() == null) {
            return false;
        }
        final String returnType = method.getReturnTypeExpression().toString().trim();
        return COLLECTION_TYPES.stream().anyMatch(returnType::startsWith);
    }

    private static boolean mutatesCollectionParam(J.MethodDeclaration method) {
        if (method.getReturnTypeExpression() == null) {
            return false;
        }
        final String returnType = method.getReturnTypeExpression().toString().trim();
        if (!"void".equals(returnType)) {
            return false;
        }

        return method.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .anyMatch(v -> {
                    final String typeText = v.getTypeExpression() != null
                            ? v.getTypeExpression().toString().trim() : "";
                    return COLLECTION_TYPES.stream().anyMatch(typeText::startsWith);
                });
    }
}
