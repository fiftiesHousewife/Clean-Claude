package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

final class LoopBodies {

    private static final String ADD_METHOD = "add";
    private static final String ADD_PATTERN = "add";
    private static final String FILTER_ADD_PATTERN = "filter-add";

    private LoopBodies() {}

    static Statement extractSingleStatement(Statement body) {
        if (!(body instanceof J.Block block)) {
            return body;
        }
        final List<Statement> statements = block.getStatements();
        if (statements.size() != 1) {
            return null;
        }
        return statements.getFirst();
    }

    static String classifyBody(Statement statement) {
        if (containsFlowControl(statement)) {
            return null;
        }
        if (isAddCall(statement)) {
            return ADD_PATTERN;
        }
        return classifyFilterAdd(statement);
    }

    private static String classifyFilterAdd(Statement statement) {
        if (!(statement instanceof J.If ifStmt)) {
            return null;
        }
        if (ifStmt.getElsePart() != null) {
            return null;
        }
        final Statement thenBody = extractSingleStatement(ifStmt.getThenPart());
        if (thenBody == null || !isAddCall(thenBody)) {
            return null;
        }
        return FILTER_ADD_PATTERN;
    }

    private static boolean isAddCall(Statement statement) {
        return statement instanceof J.MethodInvocation invocation
                && ADD_METHOD.equals(invocation.getSimpleName());
    }

    private static boolean containsFlowControl(Statement statement) {
        final boolean[] found = {false};
        new FlowControlDetector(found).visit(statement, 0);
        return found[0];
    }

    private static final class FlowControlDetector extends JavaIsoVisitor<Integer> {
        private final boolean[] found;

        FlowControlDetector(boolean[] found) {
            this.found = found;
        }

        @Override
        public J.Break visitBreak(J.Break breakStatement, Integer unused) {
            found[0] = true;
            return breakStatement;
        }

        @Override
        public J.Continue visitContinue(J.Continue continueStatement, Integer unused) {
            found[0] = true;
            return continueStatement;
        }

        @Override
        public J.Return visitReturn(J.Return returnStatement, Integer unused) {
            found[0] = true;
            return returnStatement;
        }
    }
}
