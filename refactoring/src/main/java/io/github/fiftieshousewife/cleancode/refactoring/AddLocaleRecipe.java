package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.Set;

public class AddLocaleRecipe extends Recipe {

    private static final Set<String> LOCALE_METHODS = Set.of("toLowerCase", "toUpperCase");

    @Override
    public String getDisplayName() {
        return "Add Locale.ROOT to case conversion methods";
    }

    @Override
    public String getDescription() {
        return "Adds Locale.ROOT parameter to String.toLowerCase() and toUpperCase() calls. Fixes G26.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            private final JavaTemplate localeTemplate = JavaTemplate
                    .builder("#{any(java.lang.String)}.#{}.toLowerCase(Locale.ROOT)")
                    .imports("java.util.Locale")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (!LOCALE_METHODS.contains(m.getSimpleName())) {
                    return m;
                }

                if (m.getArguments().size() == 1 && m.getArguments().getFirst() instanceof J.Empty) {
                    maybeAddImport("java.util.Locale");
                    final JavaTemplate template = JavaTemplate
                            .builder("#{any(java.lang.String)}." + m.getSimpleName() + "(Locale.ROOT)")
                            .imports("java.util.Locale")
                            .build();
                    return template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }

                return m;
            }
        };
    }
}
