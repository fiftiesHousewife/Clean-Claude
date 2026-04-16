package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.Finding;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Map;

final class OpenRewriteFindingMapper {

    private final OpenRewriteStructuralMapper structural;
    private final OpenRewriteCommentNamingMapper commentNaming;
    private final OpenRewriteBehaviouralMapper behavioural;

    OpenRewriteFindingMapper(final Map<String, String> classNameToSourcePath) {
        final OpenRewriteFindingFactory factory = new OpenRewriteFindingFactory(classNameToSourcePath);
        this.structural = new OpenRewriteStructuralMapper(factory);
        this.commentNaming = new OpenRewriteCommentNamingMapper(factory);
        this.behavioural = new OpenRewriteBehaviouralMapper(factory);
    }

    List<Finding> mapRecipe(final ScanningRecipe<?> recipe) {
        final List<Finding> structuralFindings = structural.map(recipe);
        if (!structuralFindings.isEmpty()) {
            return structuralFindings;
        }
        final List<Finding> commentNamingFindings = commentNaming.map(recipe);
        if (!commentNamingFindings.isEmpty()) {
            return commentNamingFindings;
        }
        return behavioural.map(recipe);
    }
}
