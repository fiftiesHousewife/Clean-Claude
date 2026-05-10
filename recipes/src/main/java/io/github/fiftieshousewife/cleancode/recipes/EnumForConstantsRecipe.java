package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

                final Map<String, J.VariableDeclarations> declsByName = new LinkedHashMap<>();
                for (final Statement stmt : c.getBody().getStatements()) {
                    if (stmt instanceof J.VariableDeclarations decl && isStaticFinal(decl)) {
                        for (final J.VariableDeclarations.NamedVariable variable : decl.getVariables()) {
                            declsByName.put(variable.getSimpleName(), decl);
                        }
                    }
                }

                final Map<String, List<String>> namesByPrefix = groupNamesByPrefix(declsByName.keySet());
                final Map<String, List<String>> qualifyingGroups = new LinkedHashMap<>();
                namesByPrefix.forEach((prefix, members) -> {
                    if (members.size() >= PREFIX_GROUP_THRESHOLD) {
                        qualifyingGroups.put(prefix, members);
                    }
                });
                if (qualifyingGroups.isEmpty()) {
                    return c;
                }

                // Only profile the constants that *might* be filtered as
                // one-shot — usually a small subset of the class's
                // static-finals — so we don't walk every method body for
                // unrelated classes.
                final java.util.Set<String> namesToProfile = new java.util.HashSet<>();
                qualifyingGroups.values().forEach(namesToProfile::addAll);
                final Map<String, ReferenceProfile> referenceProfiles = profileReferences(c, namesToProfile);

                qualifyingGroups.forEach((prefix, members) -> {
                    if (isFlatOneShotList(members, declsByName, referenceProfiles)) {
                        return;
                    }
                    acc.rows.add(new Row(c.getSimpleName(), prefix, members.size(), -1));
                });

                return c;
            }

            private boolean isStaticFinal(final J.VariableDeclarations varDecl) {
                boolean isStatic = false;
                boolean isFinal = false;
                for (final J.Modifier modifier : varDecl.getModifiers()) {
                    if (modifier.getType() == J.Modifier.Type.Static) {
                        isStatic = true;
                    } else if (modifier.getType() == J.Modifier.Type.Final) {
                        isFinal = true;
                    }
                }
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
                    final List<String> groupMembers,
                    final Map<String, J.VariableDeclarations> declsByName,
                    final Map<String, ReferenceProfile> referenceProfiles) {
                Statement sharedStatement = null;
                for (final String member : groupMembers) {
                    final J.VariableDeclarations decl = declsByName.get(member);
                    if (decl == null || isApiSurface(decl)) {
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

                    private boolean isOwnDeclaration(final Cursor cursor) {
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
                    private Statement topLevelStatementOf(final Cursor cursor) {
                        Object lastTreeValue = cursor.getValue();
                        Cursor current = cursor;
                        while (current.getParent() != null) {
                            current = current.getParent();
                            final Object value = current.getValue();
                            if (value instanceof J.Block && lastTreeValue instanceof Statement stmt) {
                                return stmt;
                            }
                            if (value instanceof Tree) {
                                lastTreeValue = value;
                            }
                        }
                        return null;
                    }
                }.visit(classDecl, profiles);
                return profiles;
            }

            private Map<String, List<String>> groupNamesByPrefix(final java.util.Set<String> names) {
                final Map<String, List<String>> grouped = new LinkedHashMap<>();
                for (final String name : names) {
                    final int underscore = name.indexOf('_');
                    if (underscore <= 0) {
                        continue;
                    }
                    grouped.computeIfAbsent(name.substring(0, underscore), k -> new ArrayList<>()).add(name);
                }
                return grouped;
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
