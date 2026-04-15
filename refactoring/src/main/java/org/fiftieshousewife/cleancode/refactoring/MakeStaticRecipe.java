package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MakeStaticRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Make methods static when they don't use instance state";
    }

    @Override
    public String getDescription() {
        return "Adds the static modifier to non-private, non-override methods that never reference this. Fixes G18.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (isStatic(m) || isPrivate(m) || isConstructor(m) || isOverride(m)) {
                    return m;
                }

                if (m.getBody() == null || bodyStatementCount(m) < 3) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && implementsInterface(enclosingClass)) {
                    return m;
                }

                if (referencesInstanceState(m)) {
                    return m;
                }

                return addStaticModifier(m);
            }
        };
    }

    private static boolean isStatic(J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
    }

    private static boolean isPrivate(J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }

    private static boolean isConstructor(J.MethodDeclaration m) {
        return m.getMethodType() != null && m.getMethodType().isConstructor();
    }

    private static boolean isOverride(J.MethodDeclaration m) {
        return m.getLeadingAnnotations().stream()
                .anyMatch(a -> "Override".equals(a.getSimpleName()));
    }

    private static boolean implementsInterface(J.ClassDeclaration classDecl) {
        return classDecl.getImplements() != null && !classDecl.getImplements().isEmpty();
    }

    private static int bodyStatementCount(J.MethodDeclaration m) {
        return m.getBody() != null ? m.getBody().getStatements().size() : 0;
    }

    private static boolean referencesInstanceState(J.MethodDeclaration m) {
        if (m.getBody() == null) {
            return false;
        }
        final AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean flag) {
                if ("this".equals(identifier.getSimpleName())) {
                    flag.set(true);
                }
                return identifier;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean flag) {
                final J.MethodInvocation mi = super.visitMethodInvocation(method, flag);
                if (mi.getSelect() == null && mi.getMethodType() != null
                        && !mi.getMethodType().hasFlags(Flag.Static)) {
                    flag.set(true);
                }
                return mi;
            }
        }.visit(m.getBody(), found);
        return found.get();
    }

    private static J.MethodDeclaration addStaticModifier(J.MethodDeclaration m) {
        final var staticMod = new J.Modifier(
                org.openrewrite.Tree.randomId(),
                Space.SINGLE_SPACE,
                org.openrewrite.marker.Markers.EMPTY,
                null,
                J.Modifier.Type.Static,
                List.of());

        final var newModifiers = new ArrayList<>(m.getModifiers());
        newModifiers.add(1, staticMod);

        return m.withModifiers(newModifiers);
    }
}
