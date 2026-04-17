package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UncheckedCastRecipe extends ScanningRecipe<UncheckedCastRecipe.Accumulator> {

    private static final String UNCHECKED = "unchecked";
    private static final String SUPPRESS_WARNINGS = "SuppressWarnings";
    private static final String UNKNOWN_CLASS = "<unknown>";
    private static final String CLASS_LEVEL_MEMBER = "<class-level>";

    public record Row(String className, String memberName, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Unchecked cast detection";
    }

    @Override
    public String getDescription() {
        return "Detects @SuppressWarnings(\"unchecked\") annotations that indicate type system workarounds.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new UncheckedCastScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    static boolean isSuppressUnchecked(final J.Annotation annotation) {
        return isSuppressWarnings(annotation) && containsUnchecked(annotation);
    }

    static boolean isSuppressWarnings(final J.Annotation annotation) {
        return annotation.getAnnotationType() instanceof J.Identifier id
                && SUPPRESS_WARNINGS.equals(id.getSimpleName());
    }

    static boolean containsUnchecked(final J.Annotation annotation) {
        final List<Expression> arguments = annotation.getArguments();
        if (arguments == null) {
            return false;
        }
        return arguments.stream().anyMatch(UncheckedCastRecipe::argumentMentionsUnchecked);
    }

    static boolean argumentMentionsUnchecked(final Expression argument) {
        if (argument instanceof J.Assignment assignment) {
            return expressionMentionsUnchecked(assignment.getAssignment());
        }
        return expressionMentionsUnchecked(argument);
    }

    static boolean expressionMentionsUnchecked(final Expression expression) {
        if (expression instanceof J.Literal literal) {
            return isUncheckedLiteral(literal);
        }
        if (expression instanceof J.NewArray newArray) {
            return arrayMentionsUnchecked(newArray);
        }
        return false;
    }

    static boolean arrayMentionsUnchecked(final J.NewArray newArray) {
        final List<Expression> initializer = newArray.getInitializer();
        if (initializer == null) {
            return false;
        }
        return initializer.stream()
                .filter(J.Literal.class::isInstance)
                .map(J.Literal.class::cast)
                .anyMatch(UncheckedCastRecipe::isUncheckedLiteral);
    }

    static boolean isUncheckedLiteral(final J.Literal literal) {
        return literal.getValue() instanceof String value && value.contains(UNCHECKED);
    }

    static final class UncheckedCastScanner extends JavaIsoVisitor<ExecutionContext> {

        private final Accumulator acc;

        UncheckedCastScanner(final Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.Annotation visitAnnotation(final J.Annotation annotation, final ExecutionContext ctx) {
            final J.Annotation ann = super.visitAnnotation(annotation, ctx);
            if (isSuppressUnchecked(ann)) {
                acc.rows.add(new Row(findEnclosingClassName(), findEnclosingMemberName(), -1));
            }
            return ann;
        }

        String findEnclosingClassName() {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl != null ? classDecl.getSimpleName() : UNKNOWN_CLASS;
        }

        String findEnclosingMemberName() {
            final J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);
            if (methodDecl != null) {
                return methodDecl.getSimpleName();
            }
            return findEnclosingVariableName();
        }

        String findEnclosingVariableName() {
            final J.VariableDeclarations varDecls = getCursor().firstEnclosing(J.VariableDeclarations.class);
            if (varDecls == null || varDecls.getVariables().isEmpty()) {
                return CLASS_LEVEL_MEMBER;
            }
            return varDecls.getVariables().getFirst().getSimpleName();
        }
    }
}
