package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

final class ImperativeLoopScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final String UNKNOWN = "<unknown>";
    private static final String FOR_EACH_PREFIX = "for-each with ";
    private static final String FOR_PREFIX = "for with ";

    private final ImperativeLoopRecipe.Accumulator acc;

    ImperativeLoopScanner(ImperativeLoopRecipe.Accumulator acc) {
        this.acc = acc;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, ExecutionContext ctx) {
        final J.ForEachLoop loop = super.visitForEachLoop(forEachLoop, ctx);
        recordIfClassifiable(loop.getBody(), FOR_EACH_PREFIX);
        return loop;
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
        final J.ForLoop loop = super.visitForLoop(forLoop, ctx);
        recordIfClassifiable(loop.getBody(), FOR_PREFIX);
        return loop;
    }

    private void recordIfClassifiable(Statement loopBody, String patternPrefix) {
        final Statement body = LoopBodies.extractSingleStatement(loopBody);
        if (body == null) {
            return;
        }
        final String suffix = LoopBodies.classifyBody(body);
        if (suffix == null) {
            return;
        }
        acc.rows.add(new ImperativeLoopRecipe.Row(
                enclosingClassName(),
                enclosingMethodName(),
                patternPrefix + suffix,
                -1));
    }

    private String enclosingClassName() {
        final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return classDecl != null ? classDecl.getSimpleName() : UNKNOWN;
    }

    private String enclosingMethodName() {
        final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
        return methodDecl != null ? methodDecl.getSimpleName() : UNKNOWN;
    }
}
