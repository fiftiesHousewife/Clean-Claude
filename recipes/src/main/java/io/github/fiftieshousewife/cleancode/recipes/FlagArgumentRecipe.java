package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

public class FlagArgumentRecipe extends ScanningRecipe<FlagArgumentRecipe.Accumulator> {

    public record FlagArgumentRow(
            String className,
            String methodName,
            String paramName,
            int lineNumber,
            int paramCount
    ) {}

    public static class Accumulator {
        final List<FlagArgumentRow> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Flag argument detection (F3)";
    }

    @Override
    public String getDescription() {
        return "Detects boolean parameters on non-private methods, excluding constructors.";
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
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (BoilerplateMethodSkip.isContractMethod(m)) {
                    return m;
                }

                // F3 is about API design. Internal helpers (private,
                // package-private, protected) often pass booleans through
                // a chain of overloads; flagging them is noise. Restrict
                // F3 to public methods so the smell only fires where it
                // matters.
                if (!isPublic(m) || isConstructor(m)) {
                    return m;
                }

                String className = findEnclosingClassName();
                // m.getParameters() returns a single J.Empty for no-arg
                // methods; count only the actual VariableDeclarations.
                final int paramCount = (int) m.getParameters().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .count();

                for (Statement param : m.getParameters()) {
                    if (param instanceof J.VariableDeclarations varDecl) {
                        JavaType type = varDecl.getType();
                        if (type == JavaType.Primitive.Boolean) {
                            for (J.VariableDeclarations.NamedVariable var : varDecl.getVariables()) {
                                acc.rows.add(new FlagArgumentRow(
                                        className,
                                        m.getSimpleName(),
                                        var.getSimpleName(),
                                        -1,
                                        paramCount
                                ));
                            }
                        }
                    }
                }

                return m;
            }

            private boolean isPublic(final J.MethodDeclaration method) {
                return method.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Public);
            }

            private boolean isConstructor(final J.MethodDeclaration method) {
                return method.getMethodType() != null && method.getMethodType().isConstructor();
            }

            private String findEnclosingClassName() {
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<FlagArgumentRow> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
