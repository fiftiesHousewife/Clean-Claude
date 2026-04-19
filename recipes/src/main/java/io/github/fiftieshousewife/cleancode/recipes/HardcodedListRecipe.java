package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HardcodedListRecipe extends ScanningRecipe<HardcodedListRecipe.Accumulator> {

    private static final Set<String> COLLECTION_FACTORIES = Set.of("of", "asList");

    private final int minLiterals;

    public record Row(String className, String fieldName, int literalCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    public HardcodedListRecipe(int minLiterals) {
        this.minLiterals = minLiterals;
    }

    @Override
    public String getDisplayName() {
        return "Hardcoded list/set/map detection (G35)";
    }

    @Override
    public String getDescription() {
        return "Detects static final collections initialised with %d+ hardcoded literals that should be loaded from config."
                .formatted(minLiterals);
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

                if (!isStaticFinal(v)) {
                    return v;
                }

                v.getVariables().forEach(named -> {
                    if (named.getInitializer() instanceof J.MethodInvocation mi) {
                        final int literalCount = countLiteralsInFactoryCall(mi);
                        if (literalCount >= minLiterals) {
                            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                            final String className = classDecl != null ? classDecl.getSimpleName() : "<unknown>";
                            acc.rows.add(new Row(className, named.getSimpleName(), literalCount));
                        }
                    }
                });

                return v;
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

    private static int countLiteralsInFactoryCall(J.MethodInvocation mi) {
        if (!COLLECTION_FACTORIES.contains(mi.getSimpleName())) {
            return 0;
        }
        return (int) mi.getArguments().stream()
                .filter(J.Literal.class::isInstance)
                .count();
    }

    private static boolean isStaticFinal(J.VariableDeclarations varDecl) {
        boolean isStatic = false;
        boolean isFinal = false;
        for (final J.Modifier mod : varDecl.getModifiers()) {
            if (mod.getType() == J.Modifier.Type.Static) {
                isStatic = true;
            }
            if (mod.getType() == J.Modifier.Type.Final) {
                isFinal = true;
            }
        }
        return isStatic && isFinal;
    }
}
