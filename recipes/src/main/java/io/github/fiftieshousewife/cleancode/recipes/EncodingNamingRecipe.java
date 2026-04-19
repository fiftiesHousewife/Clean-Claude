package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class EncodingNamingRecipe extends ScanningRecipe<EncodingNamingRecipe.Accumulator> {

    private static final Pattern HUNGARIAN_PREFIX = Pattern.compile("^(str|int|bool|dbl|flt|lng|obj|arr|lst|m_)[A-Z].*");
    private static final Pattern I_PREFIX_INTERFACE = Pattern.compile("^I[A-Z][a-z].*");

    public record Row(String elementKind, String className, String elementName,
                      String violationType, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Encoding naming detection (N6)";
    }

    @Override
    public String getDescription() {
        return "Detects Hungarian notation prefixes and I-prefix on interfaces.";
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

                if (c.getKind() == J.ClassDeclaration.Kind.Type.Interface
                        && I_PREFIX_INTERFACE.matcher(c.getSimpleName()).matches()) {
                    acc.rows.add(new Row("interface", c.getSimpleName(), c.getSimpleName(),
                            "I-prefix", -1));
                }

                return c;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                final J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                final String name = v.getSimpleName();

                if (HUNGARIAN_PREFIX.matcher(name).matches()) {
                    final String className = findEnclosingClassName();
                    acc.rows.add(new Row("variable", className, name, "Hungarian notation", -1));
                }

                return v;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
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
