package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.refactoring.AddFinalRecipe;
import org.fiftieshousewife.cleancode.refactoring.ChainConsecutiveBuilderCallsRecipe;
import org.fiftieshousewife.cleancode.refactoring.CollapseSiblingGuardsRecipe;
import org.fiftieshousewife.cleancode.refactoring.DeleteSectionCommentsRecipe;
import org.fiftieshousewife.cleancode.refactoring.InvertNegativeConditionalRecipe;
import org.fiftieshousewife.cleancode.refactoring.MakeMethodStaticRecipe;
import org.fiftieshousewife.cleancode.refactoring.MathMinCapRecipe;
import org.fiftieshousewife.cleancode.refactoring.MergeInlineValidationRecipe;
import org.fiftieshousewife.cleancode.refactoring.ReplaceForAddNCopiesRecipe;
import org.fiftieshousewife.cleancode.refactoring.RestoreInterruptFlagRecipe;
import org.fiftieshousewife.cleancode.refactoring.ReturnInsteadOfMutateArgRecipe;
import org.fiftieshousewife.cleancode.refactoring.ShortenFullyQualifiedReferencesRecipe;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies every deterministic refactoring recipe to each target file
 * before the agent sees it, for the {@link RunVariant#HARNESS_RECIPES_THEN_AGENT}
 * variant. Runs entirely in-process via OpenRewrite; no Gradle detour.
 *
 * <p>The pass is conservative: recipes that run are ones the team has
 * tested against a mature sandbox corpus and trusts to land without LLM
 * judgement. Anything requiring naming choices, record design, or
 * type redesign is explicitly left to the agent.
 */
public final class HarnessRecipePass {

    /** Keeps the recipe list in one place so the variant prompt stays in sync. */
    private static final List<Recipe> DETERMINISTIC_RECIPES = List.of(
            new MakeMethodStaticRecipe(),
            new RestoreInterruptFlagRecipe(),
            new DeleteSectionCommentsRecipe(),
            new ChainConsecutiveBuilderCallsRecipe(),
            new MathMinCapRecipe(),
            new ReplaceForAddNCopiesRecipe(),
            new CollapseSiblingGuardsRecipe(),
            new MergeInlineValidationRecipe(),
            new ReturnInsteadOfMutateArgRecipe(),
            new AddFinalRecipe(),
            new InvertNegativeConditionalRecipe(),
            new ShortenFullyQualifiedReferencesRecipe());

    private HarnessRecipePass() {}

    public record PassSummary(Map<Path, List<String>> recipeNamesByFile) {
        public int filesChanged() {
            return recipeNamesByFile.size();
        }

        public List<String> allRecipeNames() {
            final List<String> flat = new ArrayList<>();
            recipeNamesByFile.values().forEach(flat::addAll);
            return flat;
        }
    }

    public static PassSummary apply(final List<Path> files) throws IOException {
        final Map<Path, List<String>> byFile = new LinkedHashMap<>();
        for (final Path file : files) {
            final List<String> fired = applyToFile(file);
            if (!fired.isEmpty()) {
                byFile.put(file, fired);
            }
        }
        return new PassSummary(byFile);
    }

    private static List<String> applyToFile(final Path file) throws IOException {
        String current = Files.readString(file);
        final List<String> fired = new ArrayList<>();
        for (final Recipe recipe : DETERMINISTIC_RECIPES) {
            final String after = runOne(file, current, recipe);
            if (!after.equals(current)) {
                fired.add(recipe.getClass().getSimpleName());
                current = after;
            }
        }
        if (!fired.isEmpty()) {
            Files.writeString(file, current);
        }
        return fired;
    }

    private static String runOne(final Path file, final String source, final Recipe recipe) {
        final InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final Parser.Input input = Parser.Input.fromString(file, source);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parseInputs(List.of(input), null, ctx)
                .toList();
        if (parsed.isEmpty()) {
            return source;
        }
        final InMemoryLargeSourceSet sourceSet = new InMemoryLargeSourceSet(parsed);
        final List<Result> results = recipe.run(sourceSet, ctx).getChangeset().getAllResults();
        if (results.isEmpty() || results.get(0).getAfter() == null) {
            return source;
        }
        return results.get(0).getAfter().printAll();
    }
}
