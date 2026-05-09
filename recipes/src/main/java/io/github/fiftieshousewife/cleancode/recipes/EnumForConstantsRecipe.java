package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openrewrite.java.tree.Statement;

public class EnumForConstantsRecipe extends ScanningRecipe<EnumForConstantsRecipe.Accumulator> {

    private static final int PREFIX_GROUP_THRESHOLD = 3;

    public record Row(String className, String prefix, int fieldCount, int lineNumber) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Enum for constants detection (J3)";
    }

    @Override
    public String getDescription() {
        return "Detects groups of static final fields sharing a common prefix that should be an enum.";
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
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                final List<J.VariableDeclarations> staticFinalDecls = c.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.VariableDeclarations)
                        .map(s -> (J.VariableDeclarations) s)
                        .filter(this::isStaticFinal)
                        .toList();

                final Map<String, List<J.VariableDeclarations>> declsByName = new HashMap<>();
                for (final J.VariableDeclarations decl : staticFinalDecls) {
                    for (final J.VariableDeclarations.NamedVariable variable : decl.getVariables()) {
                        declsByName.put(variable.getSimpleName(), List.of(decl));
                    }
                }

                final List<String> constantNames = staticFinalDecls.stream()
                        .flatMap(v -> v.getVariables().stream())
                        .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                        .toList();

                final Map<String, ReferenceProfile> referenceProfiles = profileReferences(c, declsByName.keySet());

                findPrefixGroups(constantNames).forEach((prefix, count) -> {
                    if (count >= PREFIX_GROUP_THRESHOLD) {
                        if (isFlatOneShotList(prefix, constantNames, declsByName, referenceProfiles)) {
                            return;
                        }
                        acc.rows.add(new Row(c.getSimpleName(), prefix, count.intValue(), -1));
                    }
                });

                return c;
            }

            private boolean isStaticFinal(final J.VariableDeclarations varDecl) {
                final boolean isStatic = varDecl.getModifiers().stream()
                        .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
                final boolean isFinal = varDecl.getModifiers().stream()
                        .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
                return isStatic && isFinal;
            }

            /**
             * J3.1 — skip the enum suggestion when every member of the
             * prefix group is (a) {@code private} or package-private, (b)
             * referenced exactly once in the class body, and (c) every
             * reference shares the same enclosing statement (i.e. they're
             * all leaves of a single expression). Together these mean the
             * constants are a flat one-shot list — the enum overhead
             * ({@code values()}, constructor, lookup) wouldn't pay back.
             *
             * <p>If references scatter across multiple methods, the enum
             * shape pays back; the rule fires. If any constant is
             * {@code public}/{@code protected}, it's an API surface
             * consumers iterate over — also fires.
             */
            private boolean isFlatOneShotList(
                    final String prefix,
                    final List<String> allConstantNames,
                    final Map<String, List<J.VariableDeclarations>> declsByName,
                    final Map<String, ReferenceProfile> referenceProfiles) {
                final List<String> groupMembers = allConstantNames.stream()
                        .filter(n -> n.startsWith(prefix + "_"))
                        .toList();

                Statement sharedStatement = null;
                for (final String member : groupMembers) {
                    final List<J.VariableDeclarations> decls = declsByName.get(member);
                    if (decls == null || decls.isEmpty()) {
                        return false;
                    }
                    if (isApiSurface(decls.getFirst())) {
                        return false;
                    }
                    final ReferenceProfile profile = referenceProfiles.get(member);
                    if (profile == null || profile.referenceCount != 1
                            || profile.enclosingStatement == null) {
                        return false;
                    }
                    if (sharedStatement == null) {
                        sharedStatement = profile.enclosingStatement;
                    } else if (!Objects.equals(sharedStatement, profile.enclosingStatement)) {
                        return false;
                    }
                }
                return sharedStatement != null;
            }

            private boolean isApiSurface(final J.VariableDeclarations decl) {
                return decl.getModifiers().stream().anyMatch(m ->
                        m.getType() == J.Modifier.Type.Public || m.getType() == J.Modifier.Type.Protected);
            }

            private Map<String, ReferenceProfile> profileReferences(
                    final J.ClassDeclaration classDecl,
                    final java.util.Set<String> trackedNames) {
                final Map<String, ReferenceProfile> profiles = new HashMap<>();
                for (final String name : trackedNames) {
                    profiles.put(name, new ReferenceProfile());
                }
                new JavaIsoVisitor<Map<String, ReferenceProfile>>() {
                    @Override
                    public J.Identifier visitIdentifier(final J.Identifier id, final Map<String, ReferenceProfile> p) {
                        final ReferenceProfile profile = p.get(id.getSimpleName());
                        if (profile != null && !isOwnDeclaration(getCursor())) {
                            profile.referenceCount += 1;
                            final Statement stmt = topLevelStatementOf(getCursor());
                            if (profile.referenceCount == 1) {
                                profile.enclosingStatement = stmt;
                            } else if (!Objects.equals(profile.enclosingStatement, stmt)) {
                                profile.enclosingStatement = null;
                            }
                        }
                        return id;
                    }

                    private boolean isOwnDeclaration(final org.openrewrite.Cursor cursor) {
                        return cursor.firstEnclosing(J.VariableDeclarations.NamedVariable.class) != null
                                && cursor.firstEnclosing(J.MethodDeclaration.class) == null;
                    }

                    /**
                     * Walks up to the immediate Tree child of the nearest
                     * J.Block — i.e. the top-level statement of the enclosing
                     * method body. The cursor stack can contain non-Tree
                     * markers (list wrappers, padding); we track the most
                     * recent Tree-valued ancestor and return it when we hit
                     * the Block. This makes the entire
                     * {@code return a || b || c} count as one statement,
                     * not three (J.MethodInvocation is both Expression and
                     * Statement and would otherwise short-circuit the lookup).
                     */
                    private Statement topLevelStatementOf(final org.openrewrite.Cursor cursor) {
                        Object lastTreeValue = cursor.getValue();
                        org.openrewrite.Cursor current = cursor;
                        while (current.getParent() != null) {
                            current = current.getParent();
                            final Object value = current.getValue();
                            if (value instanceof J.Block && lastTreeValue instanceof Statement stmt) {
                                return stmt;
                            }
                            if (value instanceof org.openrewrite.Tree) {
                                lastTreeValue = value;
                            }
                        }
                        return null;
                    }
                }.visit(classDecl, profiles);
                return profiles;
            }

            private Map<String, Long> findPrefixGroups(final List<String> names) {
                return names.stream()
                        .filter(n -> n.contains("_"))
                        .map(n -> n.substring(0, n.indexOf('_')))
                        .collect(Collectors.groupingBy(prefix -> prefix, Collectors.counting()));
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

    private static final class ReferenceProfile {
        int referenceCount;
        Statement enclosingStatement;
    }
}
