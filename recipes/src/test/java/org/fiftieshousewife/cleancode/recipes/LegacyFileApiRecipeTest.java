package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyFileApiRecipeTest {

    @Test
    void detectsJavaIoFileDeclaration() {
        final var recipe = new LegacyFileApiRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.File;
                public class Foo {
                    void method() {
                        File file = new File("/tmp/data.csv");
                    }
                }
                """);

        assertFalse(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsFileInputStreamConstruction() {
        final var recipe = new LegacyFileApiRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.File;
                import java.io.FileInputStream;
                import java.io.IOException;
                public class Foo {
                    void method() throws IOException {
                        FileInputStream fis = new FileInputStream(new File("/tmp/data.csv"));
                    }
                }
                """);

        assertFalse(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNioPath() {
        final var recipe = new LegacyFileApiRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.nio.file.Path;
                public class Foo {
                    void method() {
                        Path path = Path.of("/tmp/data.csv");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
