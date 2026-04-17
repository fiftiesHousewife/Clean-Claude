package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigurableDataRecipe extends ScanningRecipe<ConfigurableDataRecipe.Accumulator> {

    private static final String UNKNOWN_CLASS_NAME = "<unknown>";

    private final int minValue;

    public record Row(String className, String methodName, String literalValue) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    public ConfigurableDataRecipe(final int minValue) {
        this.minValue = minValue;
    }

    @Override
    public String getDisplayName() {
        return "Configurable data at high levels detection (G35)";
    }

    @Override
    public String getDescription() {
        return "Detects magic numeric literals in private methods that should be class-level constants.";
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
            public J.Literal visitLiteral(final J.Literal literal, final ExecutionContext ctx) {
                final J.Literal lit = super.visitLiteral(literal, ctx);
                if (isReportableMagicNumber(lit, getCursor())) {
                    acc.rows.add(toRow(lit, getCursor()));
                }
                return lit;
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

    private boolean isReportableMagicNumber(final J.Literal lit, final Cursor cursor) {
        return isNumericLiteral(lit)
                && isSignificantValue(lit)
                && isInProductionClass(cursor)
                && isInPrivateMethod(cursor)
                && isInExecutableStatement(cursor);
    }

    private Row toRow(final J.Literal lit, final Cursor cursor) {
        final J.ClassDeclaration enclosingClass = cursor.firstEnclosing(J.ClassDeclaration.class);
        final J.MethodDeclaration enclosingMethod = cursor.firstEnclosing(J.MethodDeclaration.class);
        final String className = enclosingClass != null ? enclosingClass.getSimpleName() : UNKNOWN_CLASS_NAME;
        return new Row(className, enclosingMethod.getSimpleName(), lit.getValueSource());
    }

    private static boolean isNumericLiteral(final J.Literal lit) {
        return lit.getType() instanceof JavaType.Primitive p
                && (p == JavaType.Primitive.Int || p == JavaType.Primitive.Long
                || p == JavaType.Primitive.Double || p == JavaType.Primitive.Float);
    }

    boolean isSignificantValue(final J.Literal lit) {
        return lit.getValue() instanceof Number n
                && Math.abs(n.doubleValue()) > minValue;
    }

    private static boolean isInProductionClass(final Cursor cursor) {
        final J.ClassDeclaration enclosingClass = cursor.firstEnclosing(J.ClassDeclaration.class);
        return enclosingClass != null && !enclosingClass.getSimpleName().endsWith("Test");
    }

    private static boolean isInPrivateMethod(final Cursor cursor) {
        final J.MethodDeclaration enclosingMethod = cursor.firstEnclosing(J.MethodDeclaration.class);
        return enclosingMethod != null && isPrivate(enclosingMethod);
    }

    private static boolean isInExecutableStatement(final Cursor cursor) {
        final J.VariableDeclarations varDecls = cursor.firstEnclosing(J.VariableDeclarations.class);
        return varDecls == null || !isConstantDeclaration(varDecls);
    }

    private static boolean isPrivate(final J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }

    private static boolean isConstantDeclaration(final J.VariableDeclarations varDecls) {
        final boolean isStatic = varDecls.getModifiers().stream()
                .anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
        final boolean isFinal = varDecls.getModifiers().stream()
                .anyMatch(mod -> mod.getType() == J.Modifier.Type.Final);
        return isStatic && isFinal;
    }
}
