package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

public class RecordToLombokValueRecipe extends Recipe {

    private static final String VALUE_ANNOTATION = "Value";
    private static final String BUILDER_ANNOTATION = "Builder";
    private static final String LOMBOK_VALUE_IMPORT = "lombok.Value";
    private static final String LOMBOK_BUILDER_IMPORT = "lombok.Builder";
    private static final String FIELD_PREFIX = "\n    ";

    private final int minComponents;

    @JsonCreator
    public RecordToLombokValueRecipe(@JsonProperty("minComponents") final int minComponents) {
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
        return new RecordToValueVisitor();
    }

    private class RecordToValueVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
            final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
            final List<J.VariableDeclarations> components = eligibleComponents(c);
            if (components == null) {
                return c;
            }
            registerLombokImports();
            return autoFormat(convertRecordToValueClass(c, components), ctx);
        }

        private List<J.VariableDeclarations> eligibleComponents(final J.ClassDeclaration c) {
            if (c.getKind() != J.ClassDeclaration.Kind.Type.Record || c.getPrimaryConstructor() == null) {
                return null;
            }
            final List<J.VariableDeclarations> components = c.getPrimaryConstructor().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .toList();
            return components.size() >= minComponents ? components : null;
        }

        private void registerLombokImports() {
            maybeAddImport(LOMBOK_VALUE_IMPORT);
            maybeAddImport(LOMBOK_BUILDER_IMPORT);
        }

        private J.ClassDeclaration autoFormat(final J.ClassDeclaration transformed, final ExecutionContext ctx) {
            return (J.ClassDeclaration) new AutoFormat().getVisitor()
                    .visit(transformed, ctx, getCursor().getParentOrThrow());
        }
    }

    private static J.ClassDeclaration convertRecordToValueClass(
            final J.ClassDeclaration c,
            final List<J.VariableDeclarations> components) {
        return c.withKind(J.ClassDeclaration.Kind.Type.Class)
                .withLeadingAnnotations(annotationsWithValueAndBuilder(c))
                .withPrimaryConstructor(null)
                .withBody(c.getBody().withStatements(componentsAsFields(components)));
    }

    private static List<J.Annotation> annotationsWithValueAndBuilder(final J.ClassDeclaration c) {
        final List<J.Annotation> annotations = new ArrayList<>(c.getLeadingAnnotations());
        annotations.add(buildAnnotation(VALUE_ANNOTATION));
        annotations.add(buildAnnotation(BUILDER_ANNOTATION));
        return annotations;
    }

    private static List<Statement> componentsAsFields(final List<J.VariableDeclarations> components) {
        return components.stream()
                .map(RecordToLombokValueRecipe::componentToField)
                .map(Statement.class::cast)
                .toList();
    }

    private static J.Annotation buildAnnotation(final String name) {
        return new J.Annotation(
                Tree.randomId(),
                Space.format("\n"),
                Markers.EMPTY,
                new J.Identifier(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        List.of(),
                        name,
                        null, null),
                null);
    }

    private static Statement componentToField(final J.VariableDeclarations component) {
        return component
                .withModifiers(List.of())
                .withPrefix(Space.format(FIELD_PREFIX));
    }
}
