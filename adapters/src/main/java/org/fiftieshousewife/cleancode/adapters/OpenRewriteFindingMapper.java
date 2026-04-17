package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.Finding;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class OpenRewriteFindingMapper {

    private final List<RecipeCategoryMapper> categoryMappers;

    private OpenRewriteFindingMapper(final OpenRewriteFindingBuilder builder) {
        this.categoryMappers = List.of(
                new OpenRewriteFunctionMappers(builder),
                new OpenRewriteClassMappers(builder),
                new OpenRewriteConditionalMappers(builder),
                new OpenRewriteCommentAndIdiomMappers(builder));
    }

    static OpenRewriteFindingMapper forSourceFiles(final List<SourceFile> parsed) {
        return new OpenRewriteFindingMapper(OpenRewriteFindingBuilder.forSourceFiles(parsed));
    }

    List<Finding> extractFindings(final List<ScanningRecipe<?>> recipes) {
        final List<Finding> findings = new ArrayList<>();
        recipes.forEach(recipe -> findings.addAll(mapRecipe(recipe)));
        return findings;
    }

    private List<Finding> mapRecipe(final ScanningRecipe<?> recipe) {
        return categoryMappers.stream()
                .map(mapper -> mapper.tryMap(recipe))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(List::of);
    }
}
