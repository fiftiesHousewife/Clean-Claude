package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps {@link HeuristicCode}s to the deterministic OpenRewrite recipes
 * that fix them. Used by the in-page 🔧 Fix button: when a heuristic
 * has a known fix recipe, the UI offers Fix; otherwise it offers only
 * Suppress / Disable.
 *
 * <p>Recipes are referenced by fully-qualified class name, not by
 * type, because the {@code core} module deliberately doesn't depend on
 * {@code refactoring}. The plugin's apply path resolves these names at
 * runtime via reflection.
 *
 * <p>Each entry is sourced from the recipe's class javadoc — when you
 * add a new recipe you must verify it actually targets the listed code
 * before wiring it here.
 */
public final class RefactoringRegistry {

    private static final String REFACTORING = "io.github.fiftieshousewife.cleancode.refactoring.";

    private static final Map<HeuristicCode, List<String>> RECIPES_BY_CODE = Map.ofEntries(
            // G29 — replace negative conditional / collapse sibling guards
            Map.entry(HeuristicCode.G29, List.of(
                    REFACTORING + "CollapseSiblingGuardsRecipe",
                    REFACTORING + "InvertNegativeConditionalRecipe")),
            // G31 — chain consecutive builder calls
            Map.entry(HeuristicCode.G31, List.of(REFACTORING + "ChainConsecutiveBuilderCallsRecipe")),
            // G34 — drop section comments
            Map.entry(HeuristicCode.G34, List.of(REFACTORING + "DeleteSectionCommentsRecipe")),
            // G18 deliberately has NO Fix recipe. Robert Martin's heuristic
            // is "prefer non-static methods to static methods" — a method
            // that doesn't touch instance state is more often a misplaced
            // responsibility than a candidate for `static`. Auto-applying
            // MakeMethodStaticRecipe would make the codebase MORE static,
            // which is the opposite of the heuristic. The finding still
            // surfaces; the resolution is human (relocate or accept).
            // F2 — return new value instead of mutating arg
            Map.entry(HeuristicCode.F2, List.of(REFACTORING + "ReturnInsteadOfMutateArgRecipe")),
            // G4 — try-with-resources
            Map.entry(HeuristicCode.G4, List.of(REFACTORING + "UseTryWithResourcesRecipe")),
            // G12 — clutter (unused imports + FQN shortening)
            Map.entry(HeuristicCode.G12, List.of(
                    REFACTORING + "ShortenFullyQualifiedReferencesRecipe",
                    REFACTORING + "DeleteUnusedImportRecipe")),
            // G25 — magic strings extracted to local constants
            Map.entry(HeuristicCode.G25, List.of(REFACTORING + "ExtractConstantRecipe")),
            // G35 — hardcoded list extracted to class constant
            Map.entry(HeuristicCode.G35, List.of(REFACTORING + "ExtractClassConstantRecipe")),
            // G28 — remove nested ternary
            Map.entry(HeuristicCode.G28, List.of(REFACTORING + "RemoveNestedTernaryRecipe")),
            // N1 — short variable names
            Map.entry(HeuristicCode.N1, List.of(REFACTORING + "RenameShortNameRecipe")),
            // F3 — split flag-argument methods
            Map.entry(HeuristicCode.F3, List.of(REFACTORING + "SplitFlagArgumentRecipe")),
            // G14 — encapsulate boundary / merge inline validation
            Map.entry(HeuristicCode.G14, List.of(
                    REFACTORING + "EncapsulateBoundaryRecipe",
                    REFACTORING + "MergeInlineValidationRecipe")),
            // G16 — reduce visibility
            Map.entry(HeuristicCode.G16, List.of(REFACTORING + "ReduceVisibilityRecipe")),
            // G19 — extract explanatory variable
            Map.entry(HeuristicCode.G19, List.of(REFACTORING + "ExtractExplanatoryVariableRecipe")),
            // Ch7_1 — remove mumbling logs
            Map.entry(HeuristicCode.Ch7_1, List.of(REFACTORING + "DeleteMumblingLogRecipe")),
            // G26 — add Locale to format calls (locale opt-in)
            Map.entry(HeuristicCode.G26, List.of(REFACTORING + "AddLocaleRecipe"))
    );

    private RefactoringRegistry() {}

    public static boolean hasRecipeFor(final HeuristicCode code) {
        return RECIPES_BY_CODE.containsKey(code);
    }

    public static Optional<List<String>> recipesFor(final HeuristicCode code) {
        final List<String> recipes = RECIPES_BY_CODE.get(code);
        return recipes == null ? Optional.empty() : Optional.of(recipes);
    }

    public static List<HeuristicCode> codesWithRecipes() {
        return List.copyOf(RECIPES_BY_CODE.keySet());
    }
}
