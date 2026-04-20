package io.github.fiftieshousewife.cleancode.refactoring;

import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import io.github.fiftieshousewife.cleancode.refactoring.support.ClassKinds;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared skeleton for recipes that scan a compilation unit for repeated
 * literals and hoist the ones appearing {@code minOccurrences}+ times
 * into {@code private static final} class fields.
 *
 * <p>Concrete subclasses differ only in the value kind they track
 * (String for G25, Number for G35) and the three policy methods:
 * {@link #valueIfExtractable(J.Literal)} (acts as both a type filter
 * and an extractability predicate), {@link #toConstantName(Object)}
 * (naming convention), and {@link #buildConstantField(String, Object)}
 * (the field declaration to prepend).
 */
public abstract class AbstractConstantExtractionRecipe<V>
        extends ScanningRecipe<AbstractConstantExtractionRecipe.Acc<V>> {

    protected final int minOccurrences;

    protected AbstractConstantExtractionRecipe(final int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    public static class Acc<V> {
        final Map<V, Integer> counts = new HashMap<>();
        final Set<V> existingConstants = new HashSet<>();
    }

    /**
     * Returns the extractable value carried by this literal, or
     * {@code null} if this recipe should ignore the literal (wrong
     * type, trivial value, too short, etc.). Called in two places —
     * the existing-constant scan and the literal-frequency scan — so
     * the predicate and the value projection must agree.
     */
    protected abstract V valueIfExtractable(J.Literal literal);

    protected abstract String toConstantName(V value);

    protected abstract Statement buildConstantField(String name, V value);

    @Override
    public Acc<V> getInitialValue(final ExecutionContext ctx) {
        return new Acc<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Acc<V> acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                classDecl.getBody().getStatements().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .filter(AbstractConstantExtractionRecipe::isStaticFinal)
                        .flatMap(v -> v.getVariables().stream())
                        .forEach(named -> {
                            if (named.getInitializer() instanceof J.Literal lit) {
                                final V value = valueIfExtractable(lit);
                                if (value != null) {
                                    acc.existingConstants.add(value);
                                }
                            }
                        });
                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.Literal visitLiteral(final J.Literal literal, final ExecutionContext ctx) {
                final V value = valueIfExtractable(literal);
                if (value != null && !acc.existingConstants.contains(value)) {
                    acc.counts.merge(value, 1, Integer::sum);
                }
                return literal;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Acc<V> acc) {
        final Map<V, String> toExtract = new HashMap<>();
        acc.counts.entrySet().stream()
                .filter(e -> e.getValue() >= minOccurrences)
                .forEach(e -> toExtract.put(e.getKey(), toConstantName(e.getKey())));

        if (toExtract.isEmpty()) {
            return TreeVisitor.noop();
        }

        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                // Prepending `private static final` only makes sense in a
                // regular class or enum: interfaces forbid `private` on
                // fields (they are implicitly public static final), and
                // records by convention carry only their components as
                // state — extra static constants are noise.
                if (!ClassKinds.isRegularClass(c)) {
                    return c;
                }

                final List<Statement> newStatements = new ArrayList<>();
                toExtract.forEach((value, name) -> newStatements.add(buildConstantField(name, value)));
                newStatements.addAll(c.getBody().getStatements());

                return c.withBody(c.getBody().withStatements(newStatements));
            }
        };
    }

    /**
     * Parses a field declaration from a holder class literal. Delegates
     * to {@link AstFragments#parseField(String)}; subclasses call this
     * so the test set stays indirectly covered by the abstract base.
     */
    protected static Statement parseFieldFromHolder(final String classSource) {
        return AstFragments.parseField(classSource)
                .orElseThrow(() -> new IllegalStateException(
                        "holder class did not yield a field: " + classSource));
    }

    protected static boolean isStaticFinal(final J.VariableDeclarations varDecl) {
        final boolean isStatic = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
        final boolean isFinal = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
        return isStatic && isFinal;
    }
}
