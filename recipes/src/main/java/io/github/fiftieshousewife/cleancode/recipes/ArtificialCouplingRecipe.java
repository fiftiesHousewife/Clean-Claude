package io.github.fiftieshousewife.cleancode.recipes;

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

public class ArtificialCouplingRecipe extends ScanningRecipe<ArtificialCouplingRecipe.Accumulator> {

    public record Row(String declaringClass, String constantName, String usedInClass) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
        final Map<String, Set<String>> constantsByClass = new HashMap<>();
        final Map<String, Set<String>> internalReferences = new HashMap<>();
        final Map<String, Map<String, String>> externalReferences = new HashMap<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Artificial coupling detection (G13)";
    }

    @Override
    public String getDescription() {
        return "Detects public static final constants defined in one class but only referenced from another.";
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

                if (isPublicStaticFinal(v)) {
                    final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if (enclosing != null) {
                        final String className = enclosing.getSimpleName();
                        v.getVariables().forEach(named ->
                                acc.constantsByClass
                                        .computeIfAbsent(className, k -> new HashSet<>())
                                        .add(named.getSimpleName()));
                    }
                }

                return v;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                final J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);

                if (fa.getTarget() instanceof J.Identifier targetIdent) {
                    final String targetClass = targetIdent.getSimpleName();
                    final String fieldName = fa.getSimpleName();
                    final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);

                    if (enclosing != null) {
                        final String currentClass = enclosing.getSimpleName();
                        if (targetClass.equals(currentClass)) {
                            acc.internalReferences
                                    .computeIfAbsent(currentClass, k -> new HashSet<>())
                                    .add(fieldName);
                        } else {
                            acc.externalReferences
                                    .computeIfAbsent(targetClass, k -> new HashMap<>())
                                    .putIfAbsent(fieldName, currentClass);
                        }
                    }
                }

                return fa;
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
        lastAccumulator.constantsByClass.forEach((declaringClass, constants) ->
                constants.forEach(constant -> {
                    final Set<String> internal = lastAccumulator.internalReferences
                            .getOrDefault(declaringClass, Set.of());
                    final Map<String, String> external = lastAccumulator.externalReferences
                            .getOrDefault(declaringClass, Map.of());
                    if (!internal.contains(constant) && external.containsKey(constant)) {
                        results.add(new Row(declaringClass, constant, external.get(constant)));
                    }
                }));
        return Collections.unmodifiableList(results);
    }

    private static boolean isPublicStaticFinal(J.VariableDeclarations varDecl) {
        boolean isPublic = false;
        boolean isStatic = false;
        boolean isFinal = false;
        for (final J.Modifier mod : varDecl.getModifiers()) {
            switch (mod.getType()) {
                case Public -> isPublic = true;
                case Static -> isStatic = true;
                case Final -> isFinal = true;
                default -> { }
            }
        }
        return isPublic && isStatic && isFinal;
    }
}
