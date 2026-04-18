package org.fiftieshousewife.cleancode.refactoring.movemethod;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * First pass of {@link MoveMethodRecipe}. Locates the target method in
 * the source file, validates v1 preconditions, and either fills the
 * accumulator with the captured method or flips its rejection flag.
 *
 * <p>Mirrors the pre-check phase of IntelliJ's
 * {@code MoveInstanceMethodProcessor#checkConflicts}, narrowed to
 * "static method + no sibling references".
 */
final class MoveMethodScanner extends JavaIsoVisitor<ExecutionContext> {

    private final MoveMethodState state;
    private final String file;
    private final String methodName;

    MoveMethodScanner(final MoveMethodState state, final String file, final String methodName) {
        this.state = state;
        this.file = file;
        this.methodName = methodName;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                      final ExecutionContext ctx) {
        if (state.isReady() || state.rejected) {
            return method;
        }
        final J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
        if (cu == null || !sourcePathMatches(cu.getSourcePath().toString(), file)) {
            return method;
        }
        if (!method.getSimpleName().equals(methodName)) {
            return method;
        }
        if (!isStatic(method)) {
            state.reject("method is not static");
            return method;
        }
        final J.ClassDeclaration sourceClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (sourceClass == null) {
            state.reject("no enclosing class");
            return method;
        }
        if (referencesSiblingMembers(method, sourceClass)) {
            state.reject("method body references sibling members of the source class");
            return method;
        }
        state.captured = method;
        state.sourceSimpleName = sourceClass.getSimpleName();
        return method;
    }

    static boolean sourcePathMatches(final String path, final String file) {
        return path.equals(file) || path.endsWith("/" + file) || path.endsWith(file);
    }

    static boolean isStatic(final J.MethodDeclaration method) {
        return method.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
    }

    static boolean referencesSiblingMembers(final J.MethodDeclaration method,
                                            final J.ClassDeclaration sourceClass) {
        final Set<String> siblingNames = collectSiblingNames(method, sourceClass);
        if (siblingNames.isEmpty() || method.getBody() == null) {
            return false;
        }
        final AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier identifier, final AtomicBoolean flag) {
                if (siblingNames.contains(identifier.getSimpleName())) {
                    flag.set(true);
                }
                return identifier;
            }
        }.visit(method.getBody(), found);
        return found.get();
    }

    static Set<String> collectSiblingNames(final J.MethodDeclaration method,
                                           final J.ClassDeclaration sourceClass) {
        final Set<String> names = new HashSet<>();
        for (final Statement s : sourceClass.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration other && other != method) {
                names.add(other.getSimpleName());
            } else if (s instanceof J.VariableDeclarations vd) {
                vd.getVariables().forEach(nv -> names.add(nv.getSimpleName()));
            }
        }
        return names;
    }
}
