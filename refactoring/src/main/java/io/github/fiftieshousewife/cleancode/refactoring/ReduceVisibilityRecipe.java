package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.List;

public class ReduceVisibilityRecipe extends Recipe {

    private final int minLines;

    @JsonCreator
    public ReduceVisibilityRecipe(@JsonProperty("minLines") int minLines) {
        this.minLines = minLines;
    }

    @Override
    public String getDisplayName() {
        return "Reduce private method visibility for testability";
    }

    @Override
    public String getDescription() {
        return "Changes private methods with %d+ statements to package-private for direct testing. Fixes Ch3.1."
                .formatted(minLines);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (!isPrivate(m) || m.getBody() == null) {
                    return m;
                }

                if (m.getBody().getStatements().size() < minLines) {
                    return m;
                }

                final List<J.Modifier> withoutPrivate = m.getModifiers().stream()
                        .filter(mod -> mod.getType() != J.Modifier.Type.Private)
                        .toList();

                final J.MethodDeclaration updated = m.withModifiers(withoutPrivate);
                if (m.getReturnTypeExpression() != null) {
                    return updated.withReturnTypeExpression(
                            m.getReturnTypeExpression().withPrefix(
                                    m.getModifiers().getFirst().getPrefix()));
                }
                return updated;
            }
        };
    }

    private static boolean isPrivate(J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }
}
