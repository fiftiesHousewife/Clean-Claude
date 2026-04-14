package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.stream.Collectors;

public class EnumForConstantsRecipe extends ScanningRecipe<EnumForConstantsRecipe.Accumulator> {

    private static final int PREFIX_GROUP_THRESHOLD = 3;

    public record Row(String className, String prefix, int fieldCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Enum for constants detection (J3)";
    }

    @Override
    public String getDescription() {
        return "Detects groups of static final fields sharing a common prefix that should be an enum.";
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

                final List<String> constantNames = c.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.VariableDeclarations)
                        .map(s -> (J.VariableDeclarations) s)
                        .filter(this::isStaticFinalPrimitive)
                        .flatMap(v -> v.getVariables().stream())
                        .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                        .toList();

                findPrefixGroups(constantNames).forEach((prefix, count) -> {
                    if (count >= PREFIX_GROUP_THRESHOLD) {
                        acc.rows.add(new Row(c.getSimpleName(), prefix, count.intValue(), -1));
                    }
                });

                return c;
            }

            private boolean isStaticFinalPrimitive(J.VariableDeclarations varDecl) {
                final boolean isStatic = varDecl.getModifiers().stream()
                        .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
                final boolean isFinal = varDecl.getModifiers().stream()
                        .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
                return isStatic && isFinal;
            }

            private Map<String, Long> findPrefixGroups(List<String> names) {
                return names.stream()
                        .filter(n -> n.contains("_"))
                        .map(n -> n.substring(0, n.indexOf('_')))
                        .collect(Collectors.groupingBy(prefix -> prefix, Collectors.counting()));
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
