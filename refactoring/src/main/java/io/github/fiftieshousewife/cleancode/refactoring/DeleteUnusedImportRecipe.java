package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class DeleteUnusedImportRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Delete unused imports and expand star imports";
    }

    @Override
    public String getDescription() {
        return "Removes unused imports and replaces wildcard imports with explicit ones. Fixes G12 and J1.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new org.openrewrite.java.RemoveUnusedImports().getVisitor();
    }
}
