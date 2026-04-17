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
import java.util.Map;
import java.util.Set;

public class ExtractClassConstantRecipe extends ScanningRecipe<ExtractClassConstantRecipe.Acc> {

    private static final Set<Number> TRIVIAL_VALUES = Set.of(0, 1, -1, 0L, 1L, -1L, 0.0, 1.0);

    private final int minOccurrences;

    @JsonCreator
    public ExtractClassConstantRecipe(@JsonProperty("minOccurrences") int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    @Override
    public String getDisplayName() {
        return "Extract repeated numeric literals to class constants";
    }

    @Override
    public String getDescription() {
        return "Promotes numeric literals used %d+ times inside a class to private static final fields. "
                .formatted(minOccurrences)
                + "Skips 0, 1, -1 and their long/double variants. Fixes G35.";
    }

    static class Acc {
        final Map<Object, Integer> counts = new HashMap<>();
        final Set<Object> existingConstants = new HashSet<>();
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
                        .filter(ExtractClassConstantRecipe::isStaticFinal)
                        .flatMap(v -> v.getVariables().stream())
                        .forEach(named -> {
                            if (named.getInitializer() instanceof J.Literal lit
                                    && isExtractable(lit.getValue())) {
                                acc.existingConstants.add(lit.getValue());
                            }
                        });
                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                final Object value = literal.getValue();
                if (isExtractable(value) && !acc.existingConstants.contains(value)) {
                    acc.counts.merge(value, 1, Integer::sum);
                }
                return literal;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Acc acc) {
        final Map<Object, String> toExtract = new HashMap<>();
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

    private static boolean isExtractable(Object value) {
        if (!(value instanceof Number)) {
            return false;
        }
        if (TRIVIAL_VALUES.contains(value)) {
            return false;
        }
        return value instanceof Integer || value instanceof Long || value instanceof Double;
    }

    private static boolean isStaticFinal(J.VariableDeclarations varDecl) {
        final boolean isStatic = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
        final boolean isFinal = varDecl.getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Final);
        return isStatic && isFinal;
    }

    private static Statement buildConstantField(String name, Object value) {
        final String javaType;
        final String literal;
        if (value instanceof Long) {
            javaType = "long";
            literal = value + "L";
        } else if (value instanceof Double) {
            javaType = "double";
            literal = value.toString();
        } else {
            javaType = "int";
            literal = value.toString();
        }
        final String classSource = "class _Tmp { private static final %s %s = %s; }"
                .formatted(javaType, name, literal);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(classSource).toList();
        final J.ClassDeclaration tmpClass = ((J.CompilationUnit) parsed.getFirst()).getClasses().getFirst();
        return tmpClass.getBody().getStatements().getFirst();
    }

    static String toConstantName(Object value) {
        final String asText = value.toString()
                .replace(".", "_")
                .replace("-", "NEG_")
                .replace("L", "");
        return "CONSTANT_" + asText;
    }
}
