package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringlyTypedDispatchRecipe extends ScanningRecipe<StringlyTypedDispatchRecipe.Accumulator> {

    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String STRING_SIMPLE_NAME = "String";

    public record Row(String className, String methodName, String parameterName, int branchCount) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Stringly-typed dispatch detection";
    }

    @Override
    public String getDescription() {
        return "Detects methods that use a String parameter to select behaviour via switch or if/else chains.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new DispatchScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    static boolean isStringType(J.VariableDeclarations varDecls) {
        if (varDecls.getType() instanceof JavaType.FullyQualified fq) {
            return JAVA_LANG_STRING.equals(fq.getFullyQualifiedName());
        }
        if (varDecls.getTypeExpression() == null) {
            return false;
        }
        final String typeExpressionText = varDecls.getTypeExpression().toString().trim();
        return STRING_SIMPLE_NAME.equals(typeExpressionText);
    }
}
