package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObsoleteCommentRecipe extends ScanningRecipe<ObsoleteCommentRecipe.Accumulator> {

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("[a-z]+[A-Z][a-zA-Z0-9]*");

    public record Row(String className, String commentPreview, String missingIdentifier) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Obsolete comment detection (C2)";
    }

    @Override
    public String getDescription() {
        return "Detects comments that reference identifiers not present in the enclosing scope.";
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
                final String className = c.getSimpleName();
                final Set<String> classIdentifiers = collectIdentifiers(c);

                c.getBody().getStatements().forEach(stmt ->
                        checkCommentsOnStatement(stmt, className, classIdentifiers, acc));

                c.getBody().getStatements().stream()
                        .filter(J.MethodDeclaration.class::isInstance)
                        .map(J.MethodDeclaration.class::cast)
                        .forEach(method -> {
                            if (method.getBody() != null) {
                                final Set<String> methodIds = new HashSet<>(classIdentifiers);
                                methodIds.addAll(collectMethodIdentifiers(method));
                                method.getBody().getStatements().forEach(stmt ->
                                        checkCommentsOnStatement(stmt, className, methodIds, acc));
                            }
                        });

                return c;
            }

            private void checkCommentsOnStatement(Statement stmt, String className,
                                                   Set<String> knownIdentifiers, Accumulator acc) {
                stmt.getComments().stream()
                        .filter(TextComment.class::isInstance)
                        .map(TextComment.class::cast)
                        .forEach(comment -> {
                            final String text = comment.getText().trim();
                            final Set<String> referenced = extractCamelCaseIdentifiers(text);
                            referenced.removeAll(knownIdentifiers);

                            if (!referenced.isEmpty()) {
                                final String missing = referenced.iterator().next();
                                final String preview = text.substring(0, Math.min(60, text.length()));
                                acc.rows.add(new Row(className, preview, missing));
                            }
                        });
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

    private static Set<String> collectIdentifiers(J.ClassDeclaration classDecl) {
        final Set<String> ids = new HashSet<>();
        ids.add(classDecl.getSimpleName());
        classDecl.getBody().getStatements().forEach(stmt -> {
            if (stmt instanceof J.VariableDeclarations v) {
                v.getVariables().forEach(named -> ids.add(named.getSimpleName()));
            }
            if (stmt instanceof J.MethodDeclaration m) {
                ids.add(m.getSimpleName());
            }
        });
        return ids;
    }

    private static Set<String> collectMethodIdentifiers(J.MethodDeclaration method) {
        final Set<String> ids = new HashSet<>();
        ids.add(method.getSimpleName());
        method.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .forEach(v -> v.getVariables().forEach(named -> ids.add(named.getSimpleName())));
        if (method.getBody() != null) {
            method.getBody().getStatements().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .forEach(v -> v.getVariables().forEach(named -> ids.add(named.getSimpleName())));
        }
        return ids;
    }

    private static Set<String> extractCamelCaseIdentifiers(String text) {
        final Set<String> ids = new HashSet<>();
        final Matcher matcher = CAMEL_CASE_PATTERN.matcher(text);
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return ids;
    }
}
