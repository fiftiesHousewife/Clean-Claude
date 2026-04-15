package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecordToLombokValueRecipe extends Recipe {

    private final int minComponents;

    @JsonCreator
    public RecordToLombokValueRecipe(@JsonProperty("minComponents") int minComponents) {
        this.minComponents = minComponents;
    }

    @Override
    public String getDisplayName() {
        return "Convert large records to @Value @Builder classes";
    }

    @Override
    public String getDescription() {
        return "Converts Java records with more than %d components to Lombok @Value @Builder classes."
                .formatted(minComponents);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getKind() != J.ClassDeclaration.Kind.Type.Record) {
                    return c;
                }

                final var params = c.getPrimaryConstructor();
                if (params == null) {
                    return c;
                }

                final List<J.VariableDeclarations> components = params.stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .toList();

                if (components.size() < minComponents) {
                    return c;
                }

                maybeAddImport("lombok.Value");
                maybeAddImport("lombok.Builder");

                final J.Annotation valueAnn = buildAnnotation("Value");
                final J.Annotation builderAnn = buildAnnotation("Builder");

                final List<J.Annotation> annotations = new ArrayList<>(c.getLeadingAnnotations());
                annotations.add(valueAnn);
                annotations.add(builderAnn);

                final List<org.openrewrite.java.tree.Statement> fields = components.stream()
                        .map(RecordToLombokValueRecipe::componentToField)
                        .collect(Collectors.toList());

                final List<org.openrewrite.java.tree.Statement> bodyStatements = new ArrayList<>(fields);

                final J.ClassDeclaration transformed = c.withKind(J.ClassDeclaration.Kind.Type.Class)
                        .withLeadingAnnotations(annotations)
                        .withPrimaryConstructor(null)
                        .withBody(c.getBody().withStatements(bodyStatements));

                return (J.ClassDeclaration) new AutoFormat().getVisitor()
                        .visit(transformed, ctx, getCursor().getParentOrThrow());
            }
        };
    }

    private static J.Annotation buildAnnotation(String name) {
        return new J.Annotation(
                org.openrewrite.Tree.randomId(),
                Space.format("\n"),
                Markers.EMPTY,
                new J.Identifier(
                        org.openrewrite.Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        List.of(),
                        name,
                        null, null),
                null);
    }

    private static org.openrewrite.java.tree.Statement componentToField(J.VariableDeclarations component) {
        return component
                .withModifiers(List.of())
                .withPrefix(Space.format("\n    "));
    }
}
