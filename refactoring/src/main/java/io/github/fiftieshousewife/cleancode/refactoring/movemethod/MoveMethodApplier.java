package io.github.fiftieshousewife.cleancode.refactoring.movemethod;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Second pass of {@link MoveMethodRecipe}. Removes the captured method
 * from its source class, appends it to the target class, and retargets
 * every matching call site's qualifier.
 *
 * <p>Mirrors the edit phase of IntelliJ's
 * {@code MoveInstanceMethodProcessor#performRefactoring} without the
 * receiver-rewriting and conflict-resolution surface area.
 */
final class MoveMethodApplier extends JavaIsoVisitor<ExecutionContext> {

    private static final Space APPENDED_METHOD_PREFIX = Space.format("\n\n    ");

    private final MoveMethodState state;
    private final String file;
    private final String methodName;
    private final String targetFqn;
    private final String targetSimpleName;

    MoveMethodApplier(final MoveMethodState state, final String file,
                      final String methodName, final String targetFqn) {
        this.state = state;
        this.file = file;
        this.methodName = methodName;
        this.targetFqn = targetFqn;
        this.targetSimpleName = targetFqn.substring(targetFqn.lastIndexOf('.') + 1);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDeclaration,
                                                    final ExecutionContext ctx) {
        J.ClassDeclaration updated = super.visitClassDeclaration(classDeclaration, ctx);
        final J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
        if (cu == null) {
            return updated;
        }
        final String path = cu.getSourcePath().toString();
        if (MoveMethodScanner.sourcePathMatches(path, file)
                && state.sourceSimpleName.equals(updated.getSimpleName())) {
            updated = removeCapturedMethod(updated);
        }
        if (updated.getSimpleName().equals(targetSimpleName) && classFqnMatches(cu, updated)) {
            updated = appendCapturedMethod(updated);
        }
        return updated;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation invocation,
                                                    final ExecutionContext ctx) {
        final J.MethodInvocation visited = super.visitMethodInvocation(invocation, ctx);
        if (!methodName.equals(visited.getSimpleName())) {
            return visited;
        }
        final Expression select = visited.getSelect();
        if (!(select instanceof J.Identifier id) || !state.sourceSimpleName.equals(id.getSimpleName())) {
            return visited;
        }
        return visited.withSelect(id.withSimpleName(targetSimpleName));
    }

    private J.ClassDeclaration removeCapturedMethod(final J.ClassDeclaration cls) {
        final List<Statement> pruned = new ArrayList<>();
        cls.getBody().getStatements().forEach(s -> {
            if (!(s instanceof J.MethodDeclaration m && methodName.equals(m.getSimpleName()))) {
                pruned.add(s);
            }
        });
        return cls.withBody(cls.getBody().withStatements(pruned));
    }

    private J.ClassDeclaration appendCapturedMethod(final J.ClassDeclaration cls) {
        final List<Statement> statements = new ArrayList<>(cls.getBody().getStatements());
        final J.MethodDeclaration ready = state.captured
                .withId(Tree.randomId())
                .withPrefix(APPENDED_METHOD_PREFIX);
        statements.add(ready);
        return cls.withBody(cls.getBody().withStatements(statements));
    }

    private boolean classFqnMatches(final J.CompilationUnit cu, final J.ClassDeclaration cls) {
        final String pkg = cu.getPackageDeclaration() == null
                ? ""
                : cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()).trim();
        final String fqn = pkg.isEmpty() ? cls.getSimpleName() : pkg + "." + cls.getSimpleName();
        return fqn.equals(targetFqn);
    }
}
