package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.refactoring.AddFinalRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ChainConsecutiveBuilderCallsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.CollapseSiblingGuardsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.DeleteMumblingLogRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.DeleteSectionCommentsRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.InvertNegativeConditionalRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.MathMinCapRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.MergeInlineValidationRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ReplaceForAddNCopiesRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ReplaceStringBuilderWithTextBlockRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.RestoreInterruptFlagRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ReturnInsteadOfMutateArgRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.ShortenFullyQualifiedReferencesRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.UseTryWithResourcesRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.support.SuperCallScanner;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 2026-05-03: re-enabled at fifties-recipes 0.8. The previous variants
    // (`SystemOutToSlf4jRecipeNoDeps` etc.) unconditionally added `@Slf4j`
    // + Lombok imports even on classes whose classpath had no Lombok,
    // producing files that wouldn't compile. 0.8 ships
    // `JavaTransformsClasspathGated`, where the @Slf4j / System.out /
    // PrintStackTrace / JulToSlf4j transforms only fire when
    // `lombok.extern.slf4j.Slf4j` is resolvable on the source's
    // classpath. Loaded via OpenRewrite's Environment so we don't have
    // to instantiate the four Java visitors directly.
    private static final String SLF4J_TRANSFORMS_RECIPE =
            "io.github.fiftieshousewife.JavaTransformsClasspathGated";

    private static Recipe loadSlf4jTransformsRecipe() {
        return Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes(SLF4J_TRANSFORMS_RECIPE);
    }

    /**
     * Builds the recipe pipeline for a given sweep. {@code
     * externalSuperCalledNames} is no longer consumed — it's still computed
     * upstream in case a future recipe needs it, but {@code
     * MakeMethodStaticRecipe} was removed from this pipeline because it
     * goes against the spirit of Robert Martin's G18 ("prefer non-static
     * methods to static methods"); auto-applying it would make the
     * codebase MORE static, not less. The standalone refactoring is still
     * in the module for tests, just not in the harness sweep.
     */
    private static List<Recipe> deterministicRecipes(final Set<String> externalSuperCalledNames) {
        return List.of(
                new RestoreInterruptFlagRecipe(),
                new DeleteSectionCommentsRecipe(),
                new DeleteMumblingLogRecipe(),
                new ChainConsecutiveBuilderCallsRecipe(),
                new ReplaceStringBuilderWithTextBlockRecipe(),
                new MathMinCapRecipe(),
                new ReplaceForAddNCopiesRecipe(),
                new CollapseSiblingGuardsRecipe(),
                new MergeInlineValidationRecipe(),
                new ReturnInsteadOfMutateArgRecipe(),
                new UseTryWithResourcesRecipe(),
                new AddFinalRecipe(),
                new InvertNegativeConditionalRecipe(),
                new ShortenFullyQualifiedReferencesRecipe(),
                loadSlf4jTransformsRecipe());
    }

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
        return apply(files, null);
    }

    /**
     * Variant that filters the deterministic recipe chain to a single recipe
     * by simple class name (e.g. {@code "DeleteSectionCommentsRecipe"}). When
     * {@code recipeFilter} is null the full chain runs. Used by the CLI for
     * cautious one-recipe-at-a-time sweeps.
     */
    public static PassSummary apply(final List<Path> files, final String recipeFilter)
            throws IOException {
        // One project-wide scan for super.X() names, so per-file recipe
        // invocations see the cross-file callers they would otherwise
        // miss. Regex-based; false positives only cause us to skip a
        // rewrite, which is always safe.
        final Set<String> superCalledNames = SuperCallScanner.scan(files);
        List<Recipe> recipes = deterministicRecipes(superCalledNames);
        if (recipeFilter != null && !recipeFilter.isEmpty()) {
            recipes = recipes.stream()
                    .filter(r -> recipeShortName(r).equals(recipeFilter))
                    .toList();
            if (recipes.isEmpty()) {
                throw new IllegalArgumentException("Unknown recipe filter '" + recipeFilter
                        + "'; expected one of the deterministic chain's simple class names");
            }
        }
        final Map<Path, List<String>> byFile = new LinkedHashMap<>();
        for (final Path file : files) {
            final List<String> fired = applyToFile(file, recipes);
            if (!fired.isEmpty()) {
                byFile.put(file, fired);
            }
        }
        return new PassSummary(byFile);
    }

    public static List<String> applyToFile(final Path file, final List<Recipe> recipes)
            throws IOException {
        String current = Files.readString(file);
        final List<String> fired = new ArrayList<>();
        for (final Recipe recipe : recipes) {
            final String after = runOne(file, current, recipe);
            if (!after.equals(current)) {
                fired.add(recipeShortName(recipe));
                current = after;
            }
        }
        if (!fired.isEmpty()) {
            Files.writeString(file, current);
        }
        return fired;
    }

    /**
     * Friendly name for the summary. For Java recipes the FQN's simple
     * tail equals the class's simple name. For YAML composites loaded via
     * {@link Environment} the FQN is the YAML's {@code name:} field, so
     * this returns the rightmost segment (e.g.
     * {@code JavaTransformsClasspathGated}) instead of an internal wrapper
     * class name.
     */
    static String recipeShortName(final Recipe recipe) {
        final String name = recipe.getName();
        if (name == null || name.isEmpty()) {
            return recipe.getClass().getSimpleName();
        }
        final int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
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
        // example SystemOutToSlf4jRecipe creates a fresh log4j2.xml.
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
