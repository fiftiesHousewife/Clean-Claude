package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

final class RecipeTestHelper {

    private RecipeTestHelper() {}

    static void runAgainst(ScanningRecipe<?> recipe, String source) {
        runAgainst(recipe, new String[]{source});
    }

    static void runAgainst(ScanningRecipe<?> recipe, String... sources) {
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(sources).toList();
        final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
    }
}
