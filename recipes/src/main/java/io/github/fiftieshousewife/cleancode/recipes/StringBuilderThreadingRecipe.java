package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flags two related StringBuilder anti-patterns surfaced by manual-1:
 * <ol>
 *   <li><b>Naming.</b> A local {@code StringBuilder}/{@code StringBuffer}
 *       variable named {@code sb} — a non-descriptive abbreviation that
 *       obscures what the builder is accumulating. Mapped to G24.</li>
 *   <li><b>Threading.</b> A method parameter of type {@code StringBuilder}
 *       or {@code StringBuffer} that the method mutates via
 *       {@code .append(...)}. This is the output-argument anti-pattern
 *       (F2) in disguise — return the string (or a list the caller joins)
 *       instead.</li>
 * </ol>
 * Paired with {@code ReplaceStringBuilderWithTextBlockRecipe} (refactor)
 * and a skill section in {@code clean-code-java-idioms} that documents
 * the positive and negative patterns.
 */
public class StringBuilderThreadingRecipe extends ScanningRecipe<StringBuilderThreadingRecipe.Accumulator> {

    public enum Kind { NAMING, THREADING }

    public record Row(Kind kind, String className, String methodName, String variableName) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private static final String STRING_BUILDER = "java.lang.StringBuilder";
    private static final String STRING_BUFFER = "java.lang.StringBuffer";
    private static final String NON_DESCRIPTIVE_NAME = "sb";

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "StringBuilder threading + naming detection (F2 + G24)";
    }

    @Override
    public String getDescription() {
        return "Flags StringBuilder/StringBuffer locals named 'sb' (G24) and methods "
                + "that mutate a StringBuilder/StringBuffer parameter via .append() (F2).";
    }

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    final J.VariableDeclarations multi, final ExecutionContext ctx) {
                final J.VariableDeclarations result = super.visitVariableDeclarations(multi, ctx);
                if (!isBuilderType(result.getType()) || enclosingMethod() == null) {
                    return result;
                }
                final J.MethodDeclaration outerMethod = enclosingMethod();
                final boolean isParameter = outerMethod.getParameters().contains(result);
                if (isParameter) {
                    return result;
                }
                for (final J.VariableDeclarations.NamedVariable var : result.getVariables()) {
                    if (NON_DESCRIPTIVE_NAME.equals(var.getSimpleName())) {
                        acc.rows.add(new Row(Kind.NAMING, enclosingClassName(),
                                outerMethod.getSimpleName(), var.getSimpleName()));
                    }
                }
                return result;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    final J.MethodDeclaration method, final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                final Set<String> builderParamNames = new HashSet<>();
                for (final Statement p : m.getParameters()) {
                    if (p instanceof J.VariableDeclarations vd && isBuilderType(vd.getType())) {
                        vd.getVariables().forEach(v -> builderParamNames.add(v.getSimpleName()));
                    }
                }
                if (builderParamNames.isEmpty() || m.getBody() == null) {
                    return m;
                }
                final Set<String> mutated = findMutatedBuilderParams(m.getBody(), builderParamNames);
                for (final String name : mutated) {
                    acc.rows.add(new Row(Kind.THREADING, enclosingClassName(),
                            m.getSimpleName(), name));
                }
                return m;
            }

            private J.MethodDeclaration enclosingMethod() {
                return getCursor().firstEnclosing(J.MethodDeclaration.class);
            }

            private String enclosingClassName() {
                final J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return c != null ? c.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }

    private static boolean isBuilderType(final JavaType type) {
        if (type instanceof JavaType.FullyQualified fq) {
            final String fqn = fq.getFullyQualifiedName();
            return STRING_BUILDER.equals(fqn) || STRING_BUFFER.equals(fqn);
        }
        return false;
    }

    private static Set<String> findMutatedBuilderParams(final J.Block body, final Set<String> names) {
        final Set<String> mutated = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(
                    final J.MethodInvocation invocation, final Set<String> collected) {
                final J.MethodInvocation result = super.visitMethodInvocation(invocation, collected);
                if (!"append".equals(result.getSimpleName())) {
                    return result;
                }
                if (result.getSelect() instanceof J.Identifier id && names.contains(id.getSimpleName())) {
                    collected.add(id.getSimpleName());
                }
                return result;
            }
        }.visit(body, mutated);
        return mutated;
    }
}
