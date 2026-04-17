package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class OpenRewriteRecipeEngine {

    private final RecipeCatalog catalog;

    OpenRewriteRecipeEngine(final RecipeThresholds thresholds) {
        this.catalog = new RecipeCatalog(thresholds);
    }

    List<Path> collectSourceFiles(final ProjectContext context) {
        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> walkJavaFiles(root, files));
        return files;
    }

    List<SourceFile> parseSourceFiles(final List<Path> files) {
        return JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(files, null, new InMemoryExecutionContext(Throwable::printStackTrace))
                .toList();
    }

    List<ScanningRecipe<?>> createRecipes() {
        return catalog.all();
    }

    void runAll(final List<SourceFile> parsed, final List<ScanningRecipe<?>> recipes) {
        recipes.forEach(recipe -> runRecipe(parsed, recipe));
    }

    private void walkJavaFiles(final Path root, final List<Path> collector) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(collector::add);
        } catch (IOException ignored) {
        }
    }

    private void runRecipe(final List<SourceFile> parsed, final ScanningRecipe<?> recipe) {
        final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
    }
}
