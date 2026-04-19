package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;

public class RenameShortNameRecipe extends Recipe {

    private final Map<String, String> renames;

    @JsonCreator
    public RenameShortNameRecipe(@JsonProperty("renames") Map<String, String> renames) {
        this.renames = renames == null ? new HashMap<>() : new HashMap<>(renames);
    }

    @Override
    public String getDisplayName() {
        return "Rename short variable names outside loops via a user-supplied map";
    }

    @Override
    public String getDescription() {
        return "For each entry `oldName -> newName` in the map, renames variables named oldName "
                + "when they are NOT the control variable of a for/for-each loop. Fixes N5. "
                + "The map must be supplied by the caller — this recipe ships no defaults, because "
                + "a good replacement name depends on what the variable holds.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                final J.VariableDeclarations.NamedVariable visited = super.visitVariable(variable, ctx);
                final String newName = renames.get(visited.getSimpleName());
                if (newName == null || insideLoopControl()) {
                    return visited;
                }
                doAfterVisit(new RenameVariable<>(visited, newName));
                return visited;
            }

            private boolean insideLoopControl() {
                return getCursor().firstEnclosing(J.ForLoop.Control.class) != null
                        || getCursor().firstEnclosing(J.ForEachLoop.Control.class) != null;
            }
        };
    }
}
