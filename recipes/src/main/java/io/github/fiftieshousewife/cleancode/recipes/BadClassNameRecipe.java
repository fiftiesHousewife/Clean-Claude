package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BadClassNameRecipe extends ScanningRecipe<BadClassNameRecipe.Accumulator> {

    private static final Set<String> BAD_SUFFIXES = Set.of(
            "Helper", "Util", "Utils", "Manager", "Processor", "Handler");

    public record Row(String className, String suffix) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Bad class name detection (N1)";
    }

    @Override
    public String getDescription() {
        return "Detects class names ending in Helper, Util, Utils, Manager, Processor, or Handler.";
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
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                final String name = c.getSimpleName();
                BAD_SUFFIXES.stream()
                        .filter(name::endsWith)
                        .filter(suffix -> !suffixIsContractMandated(c, suffix))
                        .findFirst()
                        .ifPresent(suffix -> acc.rows.add(new Row(name, suffix)));
                return c;
            }

            /**
             * True when the class implements or extends a supertype whose
             * simple name ends with the same suffix — e.g. a record that
             * {@code implements HttpHandler} legitimately ends in
             * "Handler" because the contract dictates it.
             */
            private boolean suffixIsContractMandated(final J.ClassDeclaration c, final String suffix) {
                if (c.getImplements() != null
                        && c.getImplements().stream().anyMatch(t -> typeNameEndsWith(t, suffix))) {
                    return true;
                }
                return c.getExtends() != null && typeNameEndsWith(c.getExtends(), suffix);
            }

            private boolean typeNameEndsWith(final TypeTree type, final String suffix) {
                final String typeName = simpleNameOf(type);
                return typeName != null && typeName.endsWith(suffix);
            }

            private String simpleNameOf(final NameTree type) {
                if (type instanceof J.Identifier id) {
                    return id.getSimpleName();
                }
                if (type instanceof J.FieldAccess fa) {
                    return fa.getSimpleName();
                }
                if (type instanceof J.ParameterizedType pt) {
                    return simpleNameOf(pt.getClazz());
                }
                return null;
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
}
