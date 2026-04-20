package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.refactoring.AddFinalRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ChainConsecutiveBuilderCallsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.CollapseSiblingGuardsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.DeleteMumblingLogRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.DeleteSectionCommentsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.InvertNegativeConditionalRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.MakeMethodStaticRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.MathMinCapRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.MergeInlineValidationRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ReplaceForAddNCopiesRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.RestoreInterruptFlagRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ReturnInsteadOfMutateArgRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ShortenFullyQualifiedReferencesRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.UseTryWithResourcesRecipe;
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

    // The published Lombok/Log4j2 recipe is paused as of 2026-04-20.
    // HarnessRecipePass runs per-file, so the default variant's
    // Gradle-build-file dep injection silently no-ops while the Java
    // transform still inserts @Log4j2 + log4j imports. The NoDeps variant
    // has the same problem (still does the Java transform, still assumes
    // Lombok on the classpath). Reintroduce once the recipe ships a
    // variant that either (a) adds Lombok to the calling module's build
    // out-of-band or (b) only fires when the module already declares it.
    // private static final String SYSTEM_OUT_TO_LOMBOK_RECIPE =
    //         "io.github.fiftieshousewife.SystemOutToLombokLog4jRecipeNoDeps";

    /** Keeps the recipe list in one place so the variant prompt stays in sync. */
    private static final List<Recipe> DETERMINISTIC_RECIPES = List.of(
            new MakeMethodStaticRecipe(),
            new RestoreInterruptFlagRecipe(),
            new DeleteSectionCommentsRecipe(),
            new DeleteMumblingLogRecipe(),
            new ChainConsecutiveBuilderCallsRecipe(),
            new MathMinCapRecipe(),
            new ReplaceForAddNCopiesRecipe(),
            new CollapseSiblingGuardsRecipe(),
            new MergeInlineValidationRecipe(),
            new ReturnInsteadOfMutateArgRecipe(),
            new UseTryWithResourcesRecipe(),
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
        // Recipes can produce results for files OTHER than our input — for
        // example SystemOutToLombokLog4jRecipe creates a fresh log4j2.xml.
        // Filter to the result whose source path matches our input file so
        // we don't blow our Java fixture away with someone else's output.
        for (final Result result : results) {
            final SourceFile after = result.getAfter();
            if (after == null) {
                continue;
            }
            if (after.getSourcePath().equals(file)
                    || after.getSourcePath().endsWith(file.getFileName())) {
                return after.printAll();
            }
        }
        return source;
    }
}
