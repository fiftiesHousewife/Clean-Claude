package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InconsistentNamingRecipe extends ScanningRecipe<InconsistentNamingRecipe.Accumulator> {

    private static final List<Set<String>> CONFLICTING_PREFIX_GROUPS = List.of(
            Set.of("get", "fetch", "retrieve"),
            Set.of("create", "make", "build"),
            Set.of("remove", "delete", "destroy")
    );

    public record Row(
            String className,
            String conflictingPrefixes,
            String methodNames,
            int lineNumber
    ) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Inconsistent naming detection (G11)";
    }

    @Override
    public String getDescription() {
        return "Detects classes that mix synonymous method name prefixes (e.g. get/fetch, create/make, remove/delete).";
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
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                final List<String> methodNames = c.getBody().getStatements().stream()
                        .filter(J.MethodDeclaration.class::isInstance)
                        .map(s -> ((J.MethodDeclaration) s).getSimpleName())
                        .toList();

                for (final Set<String> group : CONFLICTING_PREFIX_GROUPS) {
                    final Map<String, List<String>> prefixToMethods = new LinkedHashMap<>();
                    for (final String prefix : group) {
                        final List<String> matching = methodNames.stream()
                                .filter(name -> name.startsWith(prefix)
                                        && name.length() > prefix.length()
                                        && Character.isUpperCase(name.charAt(prefix.length())))
                                .toList();
                        if (!matching.isEmpty()) {
                            prefixToMethods.put(prefix, matching);
                        }
                    }

                    if (prefixToMethods.size() >= 2) {
                        final String conflicting = String.join("/", prefixToMethods.keySet());
                        final String methods = prefixToMethods.values().stream()
                                .flatMap(List::stream)
                                .collect(Collectors.joining(", "));
                        acc.rows.add(new Row(
                                c.getSimpleName(),
                                conflicting,
                                methods,
                                -1
                        ));
                    }
                }

                return c;
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
