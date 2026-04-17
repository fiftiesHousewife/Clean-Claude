package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.List;

public class ReduceVisibilityRecipe extends Recipe {

    private final int minLines;

    @JsonCreator
    public ReduceVisibilityRecipe(@JsonProperty("minLines") final int minLines) {
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
        return new ReduceVisibilityVisitor(minLines);
    }

    static boolean isPrivate(final J.MethodDeclaration method) {
        return method.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }

    static boolean isShorterThan(final J.MethodDeclaration method, final int minLines) {
        return method.getBody() == null || method.getBody().getStatements().size() < minLines;
    }

    static List<J.Modifier> modifiersWithoutPrivate(final J.MethodDeclaration method) {
        return method.getModifiers().stream()
                .filter(mod -> mod.getType() != J.Modifier.Type.Private)
                .toList();
    }

    static J.MethodDeclaration preservePrefixOnReturnType(
            final J.MethodDeclaration updated, final J.MethodDeclaration original) {
        if (original.getReturnTypeExpression() == null) {
            return updated;
        }
        final Space originalPrivatePrefix = original.getModifiers().getFirst().getPrefix();
        return updated.withReturnTypeExpression(
                original.getReturnTypeExpression().withPrefix(originalPrivatePrefix));
    }

    private static final class ReduceVisibilityVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final int minLines;

        ReduceVisibilityVisitor(final int minLines) {
            this.minLines = minLines;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            if (shouldSkip(visited)) {
                return visited;
            }
            return removePrivateModifier(visited);
        }

        private boolean shouldSkip(final J.MethodDeclaration method) {
            return !isPrivate(method) || isShorterThan(method, minLines);
        }

        private J.MethodDeclaration removePrivateModifier(final J.MethodDeclaration method) {
            final J.MethodDeclaration updated = method.withModifiers(modifiersWithoutPrivate(method));
            return preservePrefixOnReturnType(updated, method);
        }
    }
}
