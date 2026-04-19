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

public class FlagArgumentRecipe extends ScanningRecipe<FlagArgumentRecipe.Accumulator> {

    public record FlagArgumentRow(
            String className,
            String methodName,
            String paramName,
            int lineNumber
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
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (isPrivate(m) || isConstructor(m)) {
                    return m;
                }

                String className = findEnclosingClassName();

                for (Statement param : m.getParameters()) {
                    if (param instanceof J.VariableDeclarations varDecl) {
                        JavaType type = varDecl.getType();
                        if (type == JavaType.Primitive.Boolean) {
                            for (J.VariableDeclarations.NamedVariable var : varDecl.getVariables()) {
                                acc.rows.add(new FlagArgumentRow(
                                        className,
                                        m.getSimpleName(),
                                        var.getSimpleName(),
                                        -1
                                ));
                            }
                        }
                    }
                }

                return m;
            }

            private boolean isPrivate(J.MethodDeclaration method) {
                return method.getModifiers().stream()
                        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
            }

            private boolean isConstructor(J.MethodDeclaration method) {
                return method.getMethodType() != null && method.getMethodType().isConstructor();
            }

            private String findEnclosingClassName() {
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<FlagArgumentRow> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
