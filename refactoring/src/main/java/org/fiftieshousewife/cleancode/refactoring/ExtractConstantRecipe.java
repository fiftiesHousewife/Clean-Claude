package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExtractConstantRecipe extends ScanningRecipe<ExtractConstantRecipe.Acc> {

    private static final int MIN_STRING_LENGTH = 3;

    private final int minOccurrences;

    @JsonCreator
    public ExtractConstantRecipe(@JsonProperty("minOccurrences") int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    @Override
    public String getDisplayName() {
        return "Extract duplicated strings to named constants";
    }

    @Override
    public String getDescription() {
        return "Extracts string literals that appear %d+ times into private static final constants. Fixes G25."
                .formatted(minOccurrences);
    }

    static class Acc {
        final Map<String, Integer> counts = new HashMap<>();
        final Set<String> existingConstants = new HashSet<>();
    }

    @Override
    public Acc getInitialValue(ExecutionContext ctx) {
        return new Acc();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Acc acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                classDecl.getBody().getStatements().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .filter(ExtractConstantRecipe::isStaticFinal)
                        .flatMap(v -> v.getVariables().stream())
                        .forEach(named -> {
                            if (named.getInitializer() instanceof J.Literal lit
                                    && lit.getValue() instanceof String s) {
                                acc.existingConstants.add(s);
                            }
                        });
                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getValue() instanceof String s && s.length() >= MIN_STRING_LENGTH) {
                    if (!acc.existingConstants.contains(s)) {
                        acc.counts.merge(s, 1, Integer::sum);
                    }
                }
                return literal;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Acc acc) {
        final Map<String, String> toExtract = new HashMap<>();
        acc.counts.entrySet().stream()
                .filter(e -> e.getValue() >= minOccurrences)
                .forEach(e -> toExtract.put(e.getKey(), toConstantName(e.getKey())));

        if (toExtract.isEmpty()) {
            return TreeVisitor.noop();
        }

        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                final List<Statement> newStatements = new ArrayList<>();
                toExtract.forEach((value, name) -> newStatements.add(buildConstantField(name, value)));
                newStatements.addAll(c.getBody().getStatements());

                return c.withBody(c.getBody().withStatements(newStatements));
            }

        };
    }

    private static boolean isStaticFinal(J.VariableDeclarations varDecl) {
        final boolean isStatic = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
        final boolean isFinal = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
        return isStatic && isFinal;
    }

    private static Statement buildConstantField(String name, String value) {
        final String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        final String classSource = "class _Tmp { private static final String %s = \"%s\"; }".formatted(name, escaped);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(classSource).toList();
        final J.ClassDeclaration tmpClass = ((J.CompilationUnit) parsed.getFirst()).getClasses().getFirst();
        return tmpClass.getBody().getStatements().getFirst();
    }

    static String toConstantName(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
    }
}
