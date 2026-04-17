package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

final class LawOfDemeterScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final String UNKNOWN = "<unknown>";
    private static final int UNKNOWN_LINE = -1;

    private final int chainDepthThreshold;
    private final LawOfDemeterRecipe.Accumulator acc;

    LawOfDemeterScanner(final int chainDepthThreshold, final LawOfDemeterRecipe.Accumulator acc) {
        this.chainDepthThreshold = chainDepthThreshold;
        this.acc = acc;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit cu, final ExecutionContext ctx) {
        cu.getClasses().forEach(classDecl -> collectProjectTypes(classDecl, packagePrefix(cu)));
        return super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
        final J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
        if (isPartOfEnclosingChain()) {
            return visited;
        }
        if (isReportableChain(visited)) {
            acc.rows.add(toRow(visited));
        }
        return visited;
    }

    private boolean isReportableChain(final J.MethodInvocation invocation) {
        final int depth = MethodChain.depth(invocation);
        return depth >= chainDepthThreshold
                && !MethodChain.isFluent(invocation)
                && involvesProjectTypes(invocation);
    }

    private LawOfDemeterRecipe.Row toRow(final J.MethodInvocation invocation) {
        return new LawOfDemeterRecipe.Row(
                findEnclosingClassName(),
                findEnclosingMethodName(),
                MethodChain.asString(invocation),
                MethodChain.depth(invocation),
                UNKNOWN_LINE);
    }

    private void collectProjectTypes(final J.ClassDeclaration classDecl, final String pkg) {
        final String fqn = pkg.isEmpty()
                ? classDecl.getSimpleName()
                : pkg + "." + classDecl.getSimpleName();
        acc.projectTypeNames.add(fqn);
        classDecl.getBody().getStatements().stream()
                .filter(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .forEach(inner -> collectProjectTypes(inner, fqn));
    }

    private static String packagePrefix(final J.CompilationUnit cu) {
        if (cu.getPackageDeclaration() == null) {
            return "";
        }
        return cu.getPackageDeclaration().getExpression().toString();
    }

    private boolean involvesProjectTypes(final J.MethodInvocation invocation) {
        if (anyReturnTypeIsProjectType(invocation)) {
            return true;
        }
        final J.Identifier root = MethodChain.findRoot(invocation);
        return root != null
                && root.getType() instanceof JavaType.FullyQualified fq
                && acc.projectTypeNames.contains(fq.getFullyQualifiedName());
    }

    private boolean anyReturnTypeIsProjectType(final J.MethodInvocation invocation) {
        J.MethodInvocation current = invocation;
        while (current != null) {
            if (current.getType() instanceof JavaType.FullyQualified fq
                    && acc.projectTypeNames.contains(fq.getFullyQualifiedName())) {
                return true;
            }
            current = current.getSelect() instanceof J.MethodInvocation nested ? nested : null;
        }
        return false;
    }

    private boolean isPartOfEnclosingChain() {
        return getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation;
    }

    private String findEnclosingClassName() {
        final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return classDecl != null ? classDecl.getSimpleName() : UNKNOWN;
    }

    private String findEnclosingMethodName() {
        final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
        return methodDecl != null ? methodDecl.getSimpleName() : UNKNOWN;
    }
}
