package io.github.fiftieshousewife.cleancode.refactoring;

import io.github.fiftieshousewife.cleancode.refactoring.support.ClassKinds;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

// Simple-name allow-list of annotations that prevent the recipe from marking a
// method static: @Override (static can't override), and the JUnit 5 lifecycle
// annotations (@Test/@BeforeEach/@AfterEach/@BeforeAll/@AfterAll/@ParameterizedTest/
// @RepeatedTest/@TestFactory/@Disabled), all of which require instance methods
// when declared at the top level of a non-static test class.

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

    private static final Set<String> SKIP_ANNOTATIONS = Set.of(
            "Override",
            "Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate",
            "BeforeEach", "AfterEach", "BeforeAll", "AfterAll", "Disabled");

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
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit cu,
                                                          final ExecutionContext ctx) {
                // Scan the whole compilation unit once for method names
                // referenced via `super.xxx()` from any class in it — those
                // are overrides and the super-chain method cannot be made
                // static without breaking the call.
                final Set<String> superCalled = collectSuperCalledMethods(cu);
                getCursor().putMessage("cleanCode.superCalled", superCalled);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl,
                                                            final ExecutionContext ctx) {
                final J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (cd.getBody() == null || !ClassKinds.isRegularClass(cd)) {
                    return cd;
                }
                // Non-static inner classes can reference the enclosing
                // class's fields implicitly (e.g. `return label;` where
                // `label` is declared on the enclosing class). Collect the
                // union of this class's fields and every enclosing
                // regular-class's fields so the instance-state check
                // picks up implicit outer references.
                final Set<String> instanceFields = new HashSet<>(collectInstanceFieldNames(cd));
                final Set<String> instanceMethods = new HashSet<>(collectInstanceMethodNames(cd));
                addEnclosingClassState(getCursor(), instanceFields, instanceMethods);
                final Set<String> superCalled = superCalledFromCursor(getCursor());
                return cd.withBody(cd.getBody().withStatements(
                        rewriteMethods(cd.getBody().getStatements(), instanceFields,
                                instanceMethods, superCalled)));
            }

            private List<org.openrewrite.java.tree.Statement> rewriteMethods(
                    final List<org.openrewrite.java.tree.Statement> statements,
                    final Set<String> fields, final Set<String> instanceMethods,
                    final Set<String> superCalled) {
                final List<org.openrewrite.java.tree.Statement> out = new ArrayList<>(statements.size());
                for (final var statement : statements) {
                    if (statement instanceof J.MethodDeclaration method
                            && !superCalled.contains(method.getSimpleName())
                            && shouldMakeStatic(method, fields, instanceMethods)) {
                        out.add(addStaticModifier(method));
                    } else {
                        out.add(statement);
                    }
                }
                return out;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Set<String> superCalledFromCursor(final Cursor cursor) {
        final Object msg = cursor.getNearestMessage("cleanCode.superCalled");
        return msg instanceof Set<?> set ? (Set<String>) set : Set.of();
    }

    private static void addEnclosingClassState(final Cursor classCursor,
                                                final Set<String> fields,
                                                final Set<String> methods) {
        classCursor.getPathAsStream(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .skip(1)
                .filter(enclosing -> enclosing.getBody() != null)
                .forEach(enclosing -> {
                    fields.addAll(collectInstanceFieldNames(enclosing));
                    methods.addAll(collectInstanceMethodNames(enclosing));
                });
    }

    private static Set<String> collectSuperCalledMethods(final J.CompilationUnit cu) {
        final Set<String> names = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation m,
                                                            final Set<String> out) {
                if (m.getSelect() instanceof J.Identifier sel && "super".equals(sel.getSimpleName())) {
                    out.add(m.getSimpleName());
                }
                return super.visitMethodInvocation(m, out);
            }
        }.visit(cu, names);
        return names;
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
        if (hasSkipAnnotation(method)) {
            return false;
        }
        return !usesInstanceState(method, instanceFields, instanceMethods);
    }

    private static boolean hasSkipAnnotation(final J.MethodDeclaration method) {
        return method.getLeadingAnnotations().stream()
                .anyMatch(a -> SKIP_ANNOTATIONS.contains(a.getSimpleName()));
    }

    private static boolean usesInstanceState(final J.MethodDeclaration method,
                                             final Set<String> fields,
                                             final Set<String> instanceMethods) {
        final AtomicBoolean uses = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier ident, final AtomicBoolean flag) {
                if (!flag.get()) {
                    final String name = ident.getSimpleName();
                    // `this` / `super` in any expression position, or a
                    // bare identifier that resolves to an instance field.
                    if ("this".equals(name) || "super".equals(name) || fields.contains(name)) {
                        flag.set(true);
                    }
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
                if (flag.get()) {
                    return super.visitMethodInvocation(m, flag);
                }
                // Bare call + instance method — either declared in this
                // class or inherited (getClass, hashCode, toString, …).
                if (m.getSelect() == null
                        && (instanceMethods.contains(m.getSimpleName())
                                || isInstanceMethodByType(m.getMethodType()))) {
                    flag.set(true);
                }
                // Explicit `this.foo()` / `super.foo()`.
                if (m.getSelect() instanceof J.Identifier sel
                        && ("this".equals(sel.getSimpleName()) || "super".equals(sel.getSimpleName()))) {
                    flag.set(true);
                }
                return super.visitMethodInvocation(m, flag);
            }

            @Override
            public J.MemberReference visitMemberReference(final J.MemberReference ref,
                                                           final AtomicBoolean flag) {
                // `this::method`, `super::method` — capture the enclosing instance.
                if (!flag.get() && ref.getContaining() instanceof J.Identifier id
                        && ("this".equals(id.getSimpleName()) || "super".equals(id.getSimpleName()))) {
                    flag.set(true);
                }
                return super.visitMemberReference(ref, flag);
            }

            @Override
            public J.NewClass visitNewClass(final J.NewClass nc, final AtomicBoolean flag) {
                // `new Inner()` where Inner is a non-static inner class of
                // an enclosing class captures `this` implicitly.
                if (!flag.get() && nc.getEnclosing() == null && needsEnclosingInstance(nc.getType())) {
                    flag.set(true);
                }
                return super.visitNewClass(nc, flag);
            }
        }.visit(method.getBody(), uses);
        return uses.get();
    }

    private static boolean isInstanceMethodByType(final JavaType.Method methodType) {
        if (methodType == null) {
            return false;
        }
        return !methodType.hasFlags(Flag.Static);
    }

    private static boolean needsEnclosingInstance(final JavaType type) {
        if (!(type instanceof JavaType.Class clazz)) {
            return false;
        }
        if (clazz.getOwningClass() == null) {
            return false;
        }
        return !clazz.hasFlags(Flag.Static);
    }

    private static J.MethodDeclaration addStaticModifier(final J.MethodDeclaration method) {
        final List<J.Modifier> newModifiers = new ArrayList<>(method.getModifiers());
        // Place `static` after visibility, before return type — conventional order.
        int insertAt = 0;
        for (int i = 0; i < newModifiers.size(); i++) {
            if (isVisibility(newModifiers.get(i).getType())) {
                insertAt = i + 1;
            }
        }
        // Whitespace bookkeeping: when we insert at position 0 and there were no
        // prior modifiers, the return type's original prefix is the space between
        // the method's leading whitespace and the return type. That prefix (whatever
        // it is — usually EMPTY when there are no modifiers) now belongs to `static`;
        // the return type must pick up a single space as the separator between
        // `static` and its type name.
        final J.Modifier staticMod;
        final J.MethodDeclaration withReturnTypeAdjusted;
        if (insertAt == 0 && newModifiers.isEmpty() && method.getReturnTypeExpression() != null) {
            final Space originalReturnPrefix = method.getReturnTypeExpression().getPrefix();
            staticMod = new J.Modifier(
                    Tree.randomId(),
                    originalReturnPrefix,
                    Markers.EMPTY,
                    null,
                    J.Modifier.Type.Static,
                    List.of());
            withReturnTypeAdjusted = method.withReturnTypeExpression(
                    method.getReturnTypeExpression().withPrefix(Space.SINGLE_SPACE));
        } else {
            staticMod = new J.Modifier(
                    Tree.randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    null,
                    J.Modifier.Type.Static,
                    List.of());
            withReturnTypeAdjusted = method;
        }
        newModifiers.add(insertAt, staticMod);
        return withReturnTypeAdjusted.withModifiers(newModifiers);
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
