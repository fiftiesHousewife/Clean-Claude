package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InappropriateStaticRecipe extends ScanningRecipe<InappropriateStaticRecipe.Accumulator> {

    public record Row(String className, String methodName) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
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
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

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
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
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

    private static boolean isMain(J.MethodDeclaration m) {
        return "main".equals(m.getSimpleName());
    }

    private static boolean implementsInterface(J.ClassDeclaration classDecl) {
        return classDecl.getImplements() != null && !classDecl.getImplements().isEmpty();
    }

    private static int bodyLineCount(J.MethodDeclaration m) {
        if (m.getBody() == null) {
            return 0;
        }
        return m.getBody().getStatements().size();
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
                        && !mi.getMethodType().hasFlags(org.openrewrite.java.tree.Flag.Static)) {
                    flag.set(true);
                }
                return mi;
            }
        }.visit(m.getBody(), found);
        return found.get();
    }
}
