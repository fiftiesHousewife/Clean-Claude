package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class OpenRewriteFindingSource implements FindingSource {

    private static final String TOOL = "openrewrite";

    private static final Set<HeuristicCode> COVERED = Set.of(
            HeuristicCode.F1, HeuristicCode.F2, HeuristicCode.F3,
            HeuristicCode.C3, HeuristicCode.C5,
            HeuristicCode.Ch3_1,
            HeuristicCode.Ch7_1, HeuristicCode.Ch7_2,
            HeuristicCode.Ch10_1, HeuristicCode.Ch10_2,
            HeuristicCode.G4, HeuristicCode.G8,
            HeuristicCode.G10, HeuristicCode.G11, HeuristicCode.G14,
            HeuristicCode.G16, HeuristicCode.G19,
            HeuristicCode.G23, HeuristicCode.G25, HeuristicCode.G26,
            HeuristicCode.G28, HeuristicCode.G29,
            HeuristicCode.G30, HeuristicCode.G33, HeuristicCode.G34, HeuristicCode.G36,
            HeuristicCode.J2, HeuristicCode.J3,
            HeuristicCode.N1, HeuristicCode.N5, HeuristicCode.N6, HeuristicCode.N7,
            HeuristicCode.T1, HeuristicCode.T3, HeuristicCode.T4);

    private final OpenRewriteRecipeFactory recipeFactory;

    public OpenRewriteFindingSource() {
        this(RecipeThresholds.defaults());
    }

    public OpenRewriteFindingSource(final RecipeThresholds thresholds) {
        this.recipeFactory = new OpenRewriteRecipeFactory(thresholds);
    }

    @Override
    public String id() {
        return TOOL;
    }

    @Override
    public String displayName() {
        return "OpenRewrite";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return COVERED;
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final List<Path> javaFiles = collectSourceFiles(context);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        final List<SourceFile> parsed = parseSourceFiles(javaFiles);
        final Map<String, String> sourcePathIndex = buildSourcePathIndex(parsed);
        final List<ScanningRecipe<?>> recipes = recipeFactory.createRecipes();
        recipes.forEach(recipe -> runRecipe(parsed, recipe));
        return extractFindings(recipes, sourcePathIndex);
    }

    private Map<String, String> buildSourcePathIndex(final List<SourceFile> parsed) {
        final Map<String, String> index = new HashMap<>();
        parsed.forEach(sf -> {
            final String path = sf.getSourcePath().toString();
            final String fileName = path.contains("/")
                    ? path.substring(path.lastIndexOf('/') + 1)
                    : path;
            final String className = fileName.endsWith(".java")
                    ? fileName.substring(0, fileName.length() - 5)
                    : fileName;
            index.put(className, path);
        });
        return index;
    }

    private List<Finding> extractFindings(final List<ScanningRecipe<?>> recipes,
                                          final Map<String, String> sourcePathIndex) {
        final OpenRewriteFindingMapper mapper = new OpenRewriteFindingMapper(sourcePathIndex);
        final List<Finding> findings = new ArrayList<>();
        recipes.forEach(recipe -> findings.addAll(mapper.mapRecipe(recipe)));
        return findings;
    }

    private List<Path> collectSourceFiles(final ProjectContext context) {
        final List<Path> files = new ArrayList<>();
        context.sourceRoots().stream()
                .filter(Files::isDirectory)
                .forEach(root -> {
                    try (Stream<Path> walk = Files.walk(root)) {
                        walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
                    } catch (IOException ignored) {
                    }
                });
        return files;
    }

    private List<SourceFile> parseSourceFiles(final List<Path> files) {
        return JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(files, null, new InMemoryExecutionContext(Throwable::printStackTrace))
                .toList();
    }

    private void runRecipe(final List<SourceFile> parsed, final ScanningRecipe<?> recipe) {
        final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
    }
}
