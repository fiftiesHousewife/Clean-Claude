package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringSwitchRecipe extends ScanningRecipe<StringSwitchRecipe.Accumulator> {

    private static final int DEFAULT_MIN_CASE_COUNT = 3;

    private final int minCaseCount;

    public StringSwitchRecipe() {
        this(DEFAULT_MIN_CASE_COUNT);
    }

    public StringSwitchRecipe(final int minCaseCount) {
        this.minCaseCount = minCaseCount;
    }
    private static final String JAVA_LANG_STRING = "java.lang.String";

    public record Row(
            String className,
            String methodName,
            int caseCount,
            String selectorName,
            int lineNumber
    ) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "String switch detection (G23)";
    }

    @Override
    public String getDescription() {
        return "Detects switch statements and expressions that dispatch on String values with 3 or more cases.";
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
            public J.Switch visitSwitch(J.Switch switchStatement, ExecutionContext ctx) {
                final J.Switch s = super.visitSwitch(switchStatement, ctx);
                inspect(s.getSelector(), s.getCases().getStatements().size(), acc);
                return s;
            }

            @Override
            public J.SwitchExpression visitSwitchExpression(J.SwitchExpression switchExpression, ExecutionContext ctx) {
                final J.SwitchExpression s = super.visitSwitchExpression(switchExpression, ctx);
                inspect(s.getSelector(), s.getCases().getStatements().size(), acc);
                return s;
            }

            private void inspect(J.ControlParentheses<Expression> selector, int caseCount, Accumulator acc) {
                final Expression selectorExpr = selector.getTree();

                if (!isStringType(selectorExpr) || caseCount < minCaseCount) {
                    return;
                }

                final String selectorName = selectorExpr instanceof J.Identifier ident
                        ? ident.getSimpleName()
                        : selectorExpr.toString();

                acc.rows.add(new Row(
                        findEnclosingClassName(),
                        findEnclosingMethodName(),
                        caseCount,
                        selectorName,
                        -1
                ));
            }

            private boolean isStringType(Expression expression) {
                final JavaType type = expression.getType();
                return type instanceof JavaType.Class classType
                        && JAVA_LANG_STRING.equals(classType.getFullyQualifiedName());
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
            }

            private String findEnclosingMethodName() {
                final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return methodDecl != null ? methodDecl.getSimpleName() : "<unknown>";
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
}
