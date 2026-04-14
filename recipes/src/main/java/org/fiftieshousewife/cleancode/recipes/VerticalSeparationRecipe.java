package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerticalSeparationRecipe extends ScanningRecipe<VerticalSeparationRecipe.Accumulator> {

    private static final int DEFAULT_DISTANCE_THRESHOLD = 5;

    private final int distanceThreshold;

    public VerticalSeparationRecipe() {
        this(DEFAULT_DISTANCE_THRESHOLD);
    }

    public VerticalSeparationRecipe(final int distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    public record Row(String className, String methodName, String variableName,
                      int declarationLine, int firstUseLine, int distance) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Vertical separation detection (G10)";
    }

    @Override
    public String getDescription() {
        return "Detects local variable declarations far from their first use.";
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
                final String className = findEnclosingClassName();
                final String methodPrint = m.print(getCursor());
                final String[] lines = methodPrint.split("\n");

                m.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.VariableDeclarations)
                        .map(s -> (J.VariableDeclarations) s)
                        .forEach(varDecl -> varDecl.getVariables().forEach(v -> {
                            final String varName = v.getSimpleName();
                            final int declLine = findLineOf(lines, varName, 0);
                            final int firstUseLine = findFirstUseAfter(lines, varName, declLine + 1);

                            if (firstUseLine > 0) {
                                final int distance = firstUseLine - declLine;
                                if (distance > distanceThreshold) {
                                    acc.rows.add(new Row(className, m.getSimpleName(),
                                            varName, declLine, firstUseLine, distance));
                                }
                            }
                        }));

                return m;
            }

            private int findLineOf(String[] lines, String varName, int from) {
                for (int i = from; i < lines.length; i++) {
                    if (lines[i].contains(varName)) {
                        return i;
                    }
                }
                return -1;
            }

            private int findFirstUseAfter(String[] lines, String varName, int from) {
                for (int i = from; i < lines.length; i++) {
                    if (lines[i].contains(varName)) {
                        return i;
                    }
                }
                return -1;
            }

            private String findEnclosingClassName() {
                final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return classDecl != null ? classDecl.getSimpleName() : "<unknown>";
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
