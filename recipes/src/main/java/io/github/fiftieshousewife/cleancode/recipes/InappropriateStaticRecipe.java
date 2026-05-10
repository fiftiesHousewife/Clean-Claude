package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.fiftieshousewife.cleancode.recipes.support.BoilerplateMethodSkip;

public class InappropriateStaticRecipe extends ScanningRecipe<InappropriateStaticRecipe.Accumulator> {

    public record Row(String className, String methodName) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
        // Class simple-name → method names referenced via `Class::method`.
        // Anything in this map is potentially the target of an unbound
        // method reference whose SAM depends on the receiver-as-first-arg
        // shape; making it static would change the effective arity.
        final Map<String, Set<String>> methodReferenceTargets = new HashMap<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Inappropriate static detection (G18)";
    }

    @Override
    public String getDescription() {
        return "Detects non-private instance methods that never reference instance state.";
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
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (BoilerplateMethodSkip.isContractMethod(m)) {
                    return m;
                }

                if (isStatic(m) || isPrivate(m) || isConstructor(m) || isOverride(m) || isMain(m)) {
                    return m;
                }

                if (bodyLineCount(m) < 3) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && implementsInterface(enclosingClass)) {
                    return m;
                }

                if (!referencesInstanceState(m)) {
                    final String className = enclosingClass != null ? enclosingClass.getSimpleName() : "<unknown>";
                    acc.rows.add(new Row(className, m.getSimpleName()));
                }

                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(final J.MemberReference memberRef, final ExecutionContext ctx) {
                final J.MemberReference ref = super.visitMemberReference(memberRef, ctx);
                final String containingTypeName = simpleTypeNameOf(ref.getContaining());
                if (containingTypeName != null && ref.getReference() != null) {
                    acc.methodReferenceTargets
                            .computeIfAbsent(containingTypeName, k -> new HashSet<>())
                            .add(ref.getReference().getSimpleName());
                }
                return ref;
            }
        };
    }

    private static String simpleTypeNameOf(final org.openrewrite.java.tree.Expression containing) {
        if (containing instanceof J.Identifier id) {
            return id.getSimpleName();
        }
        if (containing instanceof J.FieldAccess fa) {
            return fa.getSimpleName();
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        if (lastAccumulator == null) {
            return List.of();
        }
        final Map<String, Set<String>> refs = lastAccumulator.methodReferenceTargets;
        final List<Row> filtered = new ArrayList<>(lastAccumulator.rows.size());
        for (final Row row : lastAccumulator.rows) {
            final Set<String> refsForType = refs.get(row.className());
            if (refsForType != null && refsForType.contains(row.methodName())) {
                continue;
            }
            filtered.add(row);
        }
        return Collections.unmodifiableList(filtered);
    }

    private static boolean isStatic(final J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
    }

    private static boolean isPrivate(final J.MethodDeclaration m) {
        return m.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
    }

    private static boolean isConstructor(final J.MethodDeclaration m) {
        return m.getMethodType() != null && m.getMethodType().isConstructor();
    }

    private static boolean isOverride(final J.MethodDeclaration m) {
        return m.getLeadingAnnotations().stream()
                .anyMatch(a -> "Override".equals(a.getSimpleName()));
    }

    private static boolean isMain(final J.MethodDeclaration m) {
        return "main".equals(m.getSimpleName());
    }

    private static boolean implementsInterface(final J.ClassDeclaration classDecl) {
        return classDecl.getImplements() != null && !classDecl.getImplements().isEmpty();
    }

    private static int bodyLineCount(final J.MethodDeclaration m) {
        if (m.getBody() == null) {
            return 0;
        }
        return m.getBody().getStatements().size();
    }

    private static boolean referencesInstanceState(final J.MethodDeclaration m) {
        if (m.getBody() == null) {
            return false;
        }
        final AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier identifier, final AtomicBoolean flag) {
                if ("this".equals(identifier.getSimpleName()) || "super".equals(identifier.getSimpleName())) {
                    flag.set(true);
                    return identifier;
                }
                // Unqualified reference that resolves to an instance field on
                // the enclosing class (e.g. `rowsParsed++`, `sessions.get(id)`).
                // Before this check we only caught literal `this.field` and
                // unqualified instance method calls — which meant a method that
                // wrote `this.rowsParsed++` as `rowsParsed++` passed the "no
                // instance state" test and was a false-positive G18.
                final org.openrewrite.java.tree.JavaType.Variable fieldType = identifier.getFieldType();
                if (fieldType != null
                        && fieldType.getOwner() instanceof org.openrewrite.java.tree.JavaType.FullyQualified
                        && !fieldType.hasFlags(org.openrewrite.java.tree.Flag.Static)) {
                    flag.set(true);
                }
                return identifier;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final AtomicBoolean flag) {
                final J.MethodInvocation mi = super.visitMethodInvocation(method, flag);
                if (mi.getSelect() != null) {
                    return mi;
                }
                // Unqualified call. If the parser resolved the type, trust it.
                // If not (no classpath, inherited from a parent we can't see),
                // assume instance binding — declaring it static could break an
                // inherited-method override, e.g. JavaIsoVisitor#getCursor.
                if (mi.getMethodType() == null
                        || !mi.getMethodType().hasFlags(org.openrewrite.java.tree.Flag.Static)) {
                    flag.set(true);
                }
                return mi;
            }
        }.visit(m.getBody(), found);
        return found.get();
    }
}
