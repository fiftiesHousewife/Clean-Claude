package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefactoringRegistryTest {

    @Test
    void mapsKnownHeuristicCodesToRefactoringRecipes() {
        assertAll(
                () -> assertTrue(RefactoringRegistry.hasRecipeFor(HeuristicCode.G29),
                        "G29 has CollapseSiblingGuards + InvertNegativeConditional"),
                () -> assertTrue(RefactoringRegistry.hasRecipeFor(HeuristicCode.G34),
                        "G34 has DeleteSectionCommentsRecipe"),
                () -> assertFalse(RefactoringRegistry.hasRecipeFor(HeuristicCode.G18),
                        "G18 deliberately has NO Fix recipe — auto-applying MakeMethodStaticRecipe "
                                + "would make the codebase MORE static, contrary to the heuristic"),
                () -> assertTrue(RefactoringRegistry.hasRecipeFor(HeuristicCode.G12),
                        "G12 has Shorten + RemoveUnusedImports"));
    }

    @Test
    void returnsAllMappedRecipesForACode() {
        final List<String> recipes = RefactoringRegistry.recipesFor(HeuristicCode.G29).orElseThrow();

        assertAll(
                () -> assertTrue(recipes.contains(
                        "io.github.fiftieshousewife.cleancode.refactoring.CollapseSiblingGuardsRecipe")),
                () -> assertTrue(recipes.contains(
                        "io.github.fiftieshousewife.cleancode.refactoring.InvertNegativeConditionalRecipe")));
    }

    @Test
    void returnsEmptyForUnmappedCodes() {
        assertFalse(RefactoringRegistry.hasRecipeFor(HeuristicCode.T1),
                "T1 (low coverage) has no deterministic fix recipe");
        assertFalse(RefactoringRegistry.recipesFor(HeuristicCode.T1).isPresent());
    }
}
