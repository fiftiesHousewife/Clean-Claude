package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawGenericRecipeTest {

    @Test
    void detectsMapStringObject() {
        final var recipe = new RawGenericRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    void process(Map<String, Object> data) {
                        String name = (String) data.get("name");
                        System.out.println(name);
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertTrue(recipe.collectedRows().getFirst().typeName().contains("Object"))
        );
    }

    @Test
    void detectsListOfObject() {
        final var recipe = new RawGenericRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void process(List<Object> items) {
                        System.out.println(items);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
    }

    @Test
    void ignoresTypedGenerics() {
        final var recipe = new RawGenericRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    void process(Map<String, Integer> data) {
                        int count = data.get("count");
                        System.out.println(count);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresWildcardGenerics() {
        final var recipe = new RawGenericRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void process(List<?> items) {
                        System.out.println(items.size());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
