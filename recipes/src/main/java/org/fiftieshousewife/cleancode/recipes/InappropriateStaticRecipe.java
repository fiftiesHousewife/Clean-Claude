package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InappropriateStaticRecipe extends ScanningRecipe<InappropriateStaticRecipe.Accumulator> {

    private static final int MIN_BODY_STATEMENTS = 3;

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
        return new MethodDeclarationScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        if (lastAccumulator == null) {
            return List.of();
        }
        return Collections.unmodifiableList(lastAccumulator.rows);
    }

    private final class MethodDeclarationScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        MethodDeclarationScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
            final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if (shouldSkip(m)) {
                return m;
            }
            final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (enclosingClass != null && implementsInterface(enclosingClass)) {
                return m;
            }
            if (!referencesInstanceState(m)) {
                acc.rows.add(new Row(classNameOf(enclosingClass), m.getSimpleName()));
            }
            return m;
        }

        private String classNameOf(final J.ClassDeclaration enclosingClass) {
            return enclosingClass != null ? enclosingClass.getSimpleName() : "<unknown>";
        }
    }

    private static boolean shouldSkip(final J.MethodDeclaration m) {
        return isStatic(m)
                || isPrivate(m)
                || isConstructor(m)
                || isOverride(m)
                || isMain(m)
                || bodyLineCount(m) < MIN_BODY_STATEMENTS;
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
        final InstanceStateDetector detector = new InstanceStateDetector();
        detector.visit(m.getBody(), null);
        return detector.found;
    }

    private static final class InstanceStateDetector extends JavaIsoVisitor<Object> {
        private boolean found;

        @Override
        public J.Identifier visitIdentifier(final J.Identifier identifier, final Object ignored) {
            if ("this".equals(identifier.getSimpleName())) {
                found = true;
            }
            return identifier;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final Object ignored) {
            final J.MethodInvocation mi = super.visitMethodInvocation(method, ignored);
            if (isUnqualifiedInstanceCall(mi)) {
                found = true;
            }
            return mi;
        }

        private static boolean isUnqualifiedInstanceCall(final J.MethodInvocation mi) {
            return mi.getSelect() == null
                    && mi.getMethodType() != null
                    && !mi.getMethodType().hasFlags(Flag.Static);
        }
    }
}
