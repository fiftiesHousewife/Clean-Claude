package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InheritConstantsRecipe extends ScanningRecipe<InheritConstantsRecipe.Accumulator> {

    public record Row(String className, String interfaceName, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Inherit constants interface detection (J2)";
    }

    @Override
    public String getDescription() {
        return "Detects classes implementing interfaces that contain only constants.";
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

                if (c.getImplements() == null) {
                    return c;
                }

                c.getImplements().forEach(iface -> {
                    final String interfaceName = iface instanceof J.Identifier id
                            ? id.getSimpleName()
                            : iface.toString();
                    acc.rows.add(new Row(c.getSimpleName(), interfaceName, -1));
                });

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
