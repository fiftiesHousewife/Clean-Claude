package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseClassDependencyRecipe extends ScanningRecipe<BaseClassDependencyRecipe.Accumulator> {

    public record Row(String className, String derivativeName) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
        final Map<String, Set<String>> subclassMap = new HashMap<>();
        final List<InstanceofCheck> instanceofChecks = new ArrayList<>();
    }

    record InstanceofCheck(String enclosingClass, String checkedType) {}

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Base class depending on derivatives detection (G7)";
    }

    @Override
    public String getDescription() {
        return "Detects base classes that reference their subclasses via instanceof or direct type references.";
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

                if (c.getExtends() != null) {
                    final String parentName = c.getExtends().toString().trim();
                    acc.subclassMap
                            .computeIfAbsent(parentName, k -> new HashSet<>())
                            .add(c.getSimpleName());
                }

                return c;
            }

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
                final J.InstanceOf io = super.visitInstanceOf(instanceOf, ctx);

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass == null) {
                    return io;
                }

                final String className = enclosingClass.getSimpleName();
                final String checkedType = extractTypeName(io.getClazz());

                if (checkedType != null && !checkedType.equals(className)) {
                    acc.instanceofChecks.add(new InstanceofCheck(className, checkedType));
                }

                return io;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        if (lastAccumulator == null) {
            return List.of();
        }
        final List<Row> results = new ArrayList<>();
        lastAccumulator.instanceofChecks.forEach(check -> {
            final Set<String> subclasses = lastAccumulator.subclassMap
                    .getOrDefault(check.enclosingClass(), Set.of());
            if (subclasses.contains(check.checkedType())) {
                results.add(new Row(check.enclosingClass(), check.checkedType()));
            }
        });
        return Collections.unmodifiableList(results);
    }

    private static String extractTypeName(J tree) {
        if (tree instanceof J.Identifier ident) {
            return ident.getSimpleName();
        }
        if (tree instanceof J.FieldAccess fa) {
            return fa.getSimpleName();
        }
        return null;
    }
}
