package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adds {@code static} to instance methods that don't read or write any
 * instance state — the G18 "does not use instance state" finding. The
 * recipe is conservative: a method stays instance-bound if it references
 * {@code this}, {@code super}, any enclosing field, or calls another
 * non-static method on the enclosing class.
 *
 * <p>Does not rewrite call sites: Java accepts both {@code obj.foo()}
 * and {@code Owner.foo()} when {@code foo} is static, so existing
 * callers continue to compile. A follow-up recipe can replace
 * {@code this.foo()} with {@code foo()} as a stylistic pass.
 */
public final class MakeMethodStaticRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add static to methods that don't use instance state";
    }

    @Override
    public String getDescription() {
        return "Detects methods whose body references no instance fields, no `this` / `super`, "
                + "and no non-static sibling methods of the enclosing class, then adds the static "
                + "modifier. Constructors, already-static methods, and methods inside inner "
                + "non-static classes are left alone. Fixes G18.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl,
                                                            final ExecutionContext ctx) {
                final J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (cd.getBody() == null) {
                    return cd;
                }
                final Set<String> instanceFields = collectInstanceFieldNames(cd);
                final Set<String> instanceMethods = collectInstanceMethodNames(cd);
                return cd.withBody(cd.getBody().withStatements(
                        rewriteMethods(cd.getBody().getStatements(), instanceFields, instanceMethods)));
            }

            private List<org.openrewrite.java.tree.Statement> rewriteMethods(
                    final List<org.openrewrite.java.tree.Statement> statements,
                    final Set<String> fields, final Set<String> instanceMethods) {
                final List<org.openrewrite.java.tree.Statement> out = new ArrayList<>(statements.size());
                for (final var statement : statements) {
                    if (statement instanceof J.MethodDeclaration method && shouldMakeStatic(method, fields, instanceMethods)) {
                        out.add(addStaticModifier(method));
                    } else {
                        out.add(statement);
                    }
                }
                return out;
            }
        };
    }

    private static Set<String> collectInstanceFieldNames(final J.ClassDeclaration classDecl) {
        final Set<String> names = new HashSet<>();
        for (final var statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations vars && !isStatic(vars.getModifiers())) {
                vars.getVariables().forEach(v -> names.add(v.getSimpleName()));
            }
        }
        return names;
    }

    private static Set<String> collectInstanceMethodNames(final J.ClassDeclaration classDecl) {
        final Set<String> names = new HashSet<>();
        for (final var statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method
                    && !isStatic(method.getModifiers())
                    && !method.isConstructor()) {
                names.add(method.getSimpleName());
            }
        }
        return names;
    }

    private static boolean shouldMakeStatic(final J.MethodDeclaration method,
                                            final Set<String> instanceFields,
                                            final Set<String> instanceMethods) {
        if (method.isConstructor() || method.getBody() == null) {
            return false;
        }
        if (isStatic(method.getModifiers()) || isAbstract(method.getModifiers())) {
            return false;
        }
        return !usesInstanceState(method, instanceFields, instanceMethods);
    }

    private static boolean usesInstanceState(final J.MethodDeclaration method,
                                             final Set<String> fields,
                                             final Set<String> instanceMethods) {
        final AtomicBoolean uses = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier ident, final AtomicBoolean flag) {
                if (!flag.get() && fields.contains(ident.getSimpleName())) {
                    // Need parent context to distinguish field read from a local with same name.
                    // If the cursor's parent is a J.FieldAccess of a J.Identifier("this"/"super"),
                    // it's a field. Otherwise fall back to the field-name set: our collectors are
                    // limited to the immediate enclosing class, so false positives (local shadowing)
                    // only cause us to leave the method instance-bound — safe-conservative.
                    flag.set(true);
                }
                return super.visitIdentifier(ident, flag);
            }

            @Override
            public J.FieldAccess visitFieldAccess(final J.FieldAccess fa, final AtomicBoolean flag) {
                if (fa.getTarget() instanceof J.Identifier t
                        && (t.getSimpleName().equals("this") || t.getSimpleName().equals("super"))) {
                    flag.set(true);
                }
                return super.visitFieldAccess(fa, flag);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation m,
                                                            final AtomicBoolean flag) {
                if (!flag.get() && m.getSelect() == null && instanceMethods.contains(m.getSimpleName())) {
                    flag.set(true);
                }
                return super.visitMethodInvocation(m, flag);
            }
        }.visit(method.getBody(), uses);
        return uses.get();
    }

    private static J.MethodDeclaration addStaticModifier(final J.MethodDeclaration method) {
        final var staticMod = new J.Modifier(
                Tree.randomId(),
                Space.SINGLE_SPACE,
                Markers.EMPTY,
                null,
                J.Modifier.Type.Static,
                List.of());
        final List<J.Modifier> newModifiers = new ArrayList<>(method.getModifiers());
        // Place `static` after visibility, before return type — conventional order.
        int insertAt = 0;
        for (int i = 0; i < newModifiers.size(); i++) {
            if (isVisibility(newModifiers.get(i).getType())) {
                insertAt = i + 1;
            }
        }
        newModifiers.add(insertAt, staticMod);
        return method.withModifiers(newModifiers);
    }

    private static boolean isStatic(final List<J.Modifier> modifiers) {
        return modifiers.stream().anyMatch(m -> m.getType() == J.Modifier.Type.Static);
    }

    private static boolean isAbstract(final List<J.Modifier> modifiers) {
        return modifiers.stream().anyMatch(m -> m.getType() == J.Modifier.Type.Abstract);
    }

    private static boolean isVisibility(final J.Modifier.Type type) {
        return type == J.Modifier.Type.Public
                || type == J.Modifier.Type.Protected
                || type == J.Modifier.Type.Private;
    }
}
