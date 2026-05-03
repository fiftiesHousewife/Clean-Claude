package io.github.fiftieshousewife.cleancode.plugin.serve;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.RefactoringRegistry;
import io.github.fiftieshousewife.cleancode.plugin.rework.HarnessRecipePass;
import io.github.fiftieshousewife.cleancode.refactoring.MakeMethodStaticRecipe;
import io.github.fiftieshousewife.cleancode.refactoring.support.SuperCallScanner;
import org.openrewrite.Recipe;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Runs the deterministic OpenRewrite recipes registered for a given
 * {@link HeuristicCode}. Used by the in-page 🔧 Fix button: when the
 * triage UI sends an {@code applyRefactoring} change with a file path,
 * we run the relevant recipes against that file; without a file we run
 * them across every {@code .java} file under the project root.
 *
 * <p>Recipe instances are reflectively constructed from the FQN strings
 * stored in {@link RefactoringRegistry} so {@code core} can stay free
 * of the {@code refactoring} module dependency.
 *
 * <p>{@link MakeMethodStaticRecipe} is special-cased because its
 * constructor needs a project-wide set of {@code super.X()} call sites
 * — without that the recipe would unsafely promote methods that are
 * legitimately overridden elsewhere.
 */
public final class RefactoringApplier {

    public record Outcome(int filesChanged, List<String> recipesFired, List<String> errors) {
        public Outcome {
            recipesFired = List.copyOf(recipesFired);
            errors = List.copyOf(errors);
        }

        public boolean success() {
            return errors.isEmpty();
        }
    }

    private final Path projectRoot;

    public RefactoringApplier(final Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public Outcome apply(final HeuristicCode code, final Optional<Path> targetFile) {
        final List<String> recipeNames = RefactoringRegistry.recipesFor(code)
                .orElseGet(List::of);
        if (recipeNames.isEmpty()) {
            return new Outcome(0, List.of(),
                    List.of("no fix recipe registered for " + code.name()));
        }

        final List<Path> targets;
        try {
            targets = targetFile.map(List::of).orElseGet(this::allJavaFiles);
        } catch (RuntimeException e) {
            return new Outcome(0, List.of(),
                    List.of("could not enumerate target files: " + e.getMessage()));
        }
        if (targets.isEmpty()) {
            return new Outcome(0, List.of(),
                    List.of("no Java files found under " + projectRoot));
        }

        final Set<String> superCalledNames = scanSuperCalls(targets);
        final List<Recipe> recipes;
        try {
            recipes = instantiateRecipes(recipeNames, superCalledNames);
        } catch (ReflectiveOperationException e) {
            return new Outcome(0, List.of(),
                    List.of("could not instantiate fix recipes for " + code.name() + ": " + e.getMessage()));
        }

        final List<String> errors = new ArrayList<>();
        final Set<String> firedAcrossAll = new LinkedHashSet<>();
        int filesChanged = 0;
        for (final Path file : targets) {
            try {
                final List<String> fired = HarnessRecipePass.applyToFile(file, recipes);
                if (!fired.isEmpty()) {
                    filesChanged++;
                    firedAcrossAll.addAll(fired);
                }
            } catch (IOException e) {
                errors.add("apply failed for " + projectRoot.relativize(file) + ": " + e.getMessage());
            }
        }
        if (filesChanged == 0 && errors.isEmpty()) {
            errors.add("no fix applied — recipe " + code.name() + " did not match anything in "
                    + (targetFile.isPresent() ? projectRoot.relativize(targetFile.get()).toString()
                            : "the project"));
        }
        return new Outcome(filesChanged, List.copyOf(firedAcrossAll), errors);
    }

    private List<Path> allJavaFiles() {
        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.gradle/"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> scanSuperCalls(final List<Path> targets) {
        try {
            return SuperCallScanner.scan(targets);
        } catch (IOException e) {
            return Set.of();
        }
    }

    private List<Recipe> instantiateRecipes(final List<String> recipeNames,
                                              final Set<String> superCalledNames) throws ReflectiveOperationException {
        final List<Recipe> recipes = new ArrayList<>();
        for (final String fqn : recipeNames) {
            recipes.add(instantiateOne(fqn, superCalledNames));
        }
        return recipes;
    }

    private Recipe instantiateOne(final String fqn, final Set<String> superCalledNames)
            throws ReflectiveOperationException {
        final Class<?> klass = Class.forName(fqn);
        if (klass == MakeMethodStaticRecipe.class) {
            return new MakeMethodStaticRecipe(superCalledNames);
        }
        final Constructor<?> ctor = klass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return (Recipe) ctor.newInstance();
    }
}
