package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RawGenericRecipe extends ScanningRecipe<RawGenericRecipe.Accumulator> {

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "Map", "List", "Set", "Collection", "HashMap", "ArrayList",
            "HashSet", "LinkedList", "TreeMap", "TreeSet");

    public record Row(String className, String methodName, String typeName) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Raw generic type detection (G26)";
    }

    @Override
    public String getDescription() {
        return "Detects collection types parameterised with Object, suggesting a stringly-typed API.";
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
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations varDecl, ExecutionContext ctx) {
                final J.VariableDeclarations v = super.visitVariableDeclarations(varDecl, ctx);
                checkTypeExpression(v.getTypeExpression(), acc);
                return v;
            }

            private void checkTypeExpression(TypeTree typeExpr, Accumulator acc) {
                if (typeExpr == null) {
                    return;
                }
                final String typeText = typeExpr.toString().trim();
                if (!containsObjectParameter(typeText)) {
                    return;
                }

                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                final String methodName = methodDecl != null ? methodDecl.getSimpleName() : "<field>";
                acc.rows.add(new Row(className, methodName, typeText));
            }

            private boolean containsObjectParameter(String typeText) {
                return COLLECTION_TYPES.stream().anyMatch(ct ->
                        typeText.startsWith(ct + "<") && typeText.contains("Object")
                        && !typeText.contains("?"));
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
