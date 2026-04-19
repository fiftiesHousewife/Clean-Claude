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
                final String name = c.getSimpleName();
                BAD_SUFFIXES.stream()
                        .filter(name::endsWith)
                        .findFirst()
                        .ifPresent(suffix -> acc.rows.add(new Row(name, suffix)));
                return c;
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
