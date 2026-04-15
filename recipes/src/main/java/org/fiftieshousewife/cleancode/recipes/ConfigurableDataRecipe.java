package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigurableDataRecipe extends ScanningRecipe<ConfigurableDataRecipe.Accumulator> {

    private static final Set<Object> TRIVIAL_VALUES = Set.of(0, 1, -1, 0L, 1L, -1L, 0.0, 1.0, 0.0f, 1.0f);

    public record Row(String className, String methodName, String literalValue) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Configurable data at high levels detection (G35)";
    }

    @Override
    public String getDescription() {
        return "Detects magic numeric literals in private methods that should be class-level constants.";
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
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                final J.Literal lit = super.visitLiteral(literal, ctx);

                if (!isNumericLiteral(lit)) {
                    return lit;
                }

                if (isTrivialValue(lit)) {
                    return lit;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && enclosingClass.getSimpleName().endsWith("Test")) {
                    return lit;
                }

                final J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod == null || !isPrivate(enclosingMethod)) {
                    return lit;
                }

                final J.VariableDeclarations varDecls = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (varDecls != null && isConstantDeclaration(varDecls)) {
                    return lit;
                }

                final String className = enclosingClass != null ? enclosingClass.getSimpleName() : "<unknown>";
                acc.rows.add(new Row(className, enclosingMethod.getSimpleName(), lit.getValueSource()));

                return lit;
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

    private static boolean isNumericLiteral(J.Literal lit) {
        if (lit.getType() == null) {
            return false;
        }
        return lit.getType() instanceof JavaType.Primitive p
                && (p == JavaType.Primitive.Int || p == JavaType.Primitive.Long
                || p == JavaType.Primitive.Double || p == JavaType.Primitive.Float);
    }

    private static boolean isTrivialValue(J.Literal lit) {
        if (lit.getValue() == null) {
            return true;
        }
        if (lit.getValue() instanceof Number n) {
            return TRIVIAL_VALUES.stream().anyMatch(t -> {
                if (t instanceof Number tn) {
                    return tn.doubleValue() == n.doubleValue();
                }
                return false;
            });
        }
        return false;
    }

    private static boolean isPrivate(J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }

    private static boolean isConstantDeclaration(J.VariableDeclarations varDecls) {
        final boolean isStatic = varDecls.getModifiers().stream()
                .anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
        final boolean isFinal = varDecls.getModifiers().stream()
                .anyMatch(mod -> mod.getType() == J.Modifier.Type.Final);
        return isStatic && isFinal;
    }
}
