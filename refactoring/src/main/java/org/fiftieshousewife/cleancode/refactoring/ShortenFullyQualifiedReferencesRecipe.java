package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;

public class ShortenFullyQualifiedReferencesRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace fully-qualified type references with imports";
    }

    @Override
    public String getDescription() {
        return "Rewrites `org.example.Foo.bar()` to `bar()` plus an `import org.example.Foo` "
                + "statement, so code uses imports instead of inline FQNs. Fixes G12.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ShortenFullyQualifiedTypeReferences().getVisitor();
    }
}
