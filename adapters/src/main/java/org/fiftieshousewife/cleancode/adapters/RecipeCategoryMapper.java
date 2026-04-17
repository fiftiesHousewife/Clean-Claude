package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.Finding;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Optional;

interface RecipeCategoryMapper {

    Optional<List<Finding>> tryMap(ScanningRecipe<?> recipe);
}
