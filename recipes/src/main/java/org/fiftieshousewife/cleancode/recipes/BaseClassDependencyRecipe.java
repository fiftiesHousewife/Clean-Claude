package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

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
        return new ScannerVisitor(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        if (lastAccumulator == null) {
            return List.of();
        }
        return buildRows(lastAccumulator);
    }

    static List<Row> buildRows(final Accumulator acc) {
        final List<Row> results = new ArrayList<>();
        acc.instanceofChecks.forEach(check -> {
            final Set<String> subclasses = acc.subclassMap.getOrDefault(check.enclosingClass(), Set.of());
            if (subclasses.contains(check.checkedType())) {
                results.add(new Row(check.enclosingClass(), check.checkedType()));
            }
        });
        return Collections.unmodifiableList(results);
    }

    static String extractTypeName(final J tree) {
        return switch (tree) {
            case J.Identifier ident -> ident.getSimpleName();
            case J.FieldAccess fa -> fa.getSimpleName();
            default -> null;
        };
    }

    private static final class ScannerVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        ScannerVisitor(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
            recordSubclass(c);
            return c;
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
            final J.InstanceOf io = super.visitInstanceOf(instanceOf, ctx);
            recordInstanceofCheck(io);
            return io;
        }

        private void recordSubclass(final J.ClassDeclaration c) {
            if (c.getExtends() == null) {
                return;
            }
            final String parentName = c.getExtends().toString().trim();
            acc.subclassMap
                    .computeIfAbsent(parentName, k -> new HashSet<>())
                    .add(c.getSimpleName());
        }

        private void recordInstanceofCheck(final J.InstanceOf io) {
            final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (enclosingClass == null) {
                return;
            }
            final String className = enclosingClass.getSimpleName();
            final String checkedType = extractTypeName(io.getClazz());
            if (checkedType != null && !checkedType.equals(className)) {
                acc.instanceofChecks.add(new InstanceofCheck(className, checkedType));
            }
        }
    }
}
