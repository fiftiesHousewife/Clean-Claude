package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringBuilderThreadingRecipeTest {

    @Test
    void flagsLocalStringBuilderNamedSbAsNamingViolation() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String render() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("x");
                        return sb.toString();
                    }
                }
                """);

        final var rows = recipe.collectedRows();
        assertAll(
                () -> assertEquals(1, rows.size()),
                () -> assertEquals(StringBuilderThreadingRecipe.Kind.NAMING, rows.getFirst().kind()),
                () -> assertEquals("Foo", rows.getFirst().className()),
                () -> assertEquals("sb", rows.getFirst().variableName()));
    }

    @Test
    void flagsLocalStringBufferNamedSbAsNamingViolation() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String render() {
                        StringBuffer sb = new StringBuffer();
                        sb.append("x");
                        return sb.toString();
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals(StringBuilderThreadingRecipe.Kind.NAMING,
                recipe.collectedRows().getFirst().kind());
    }

    @Test
    void ignoresLocalStringBuilderWithDescriptiveName() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String render() {
                        StringBuilder html = new StringBuilder();
                        html.append("<p>");
                        return html.toString();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void flagsMethodParameterOfBuilderTypeWhenAppendedTo() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private void renderRow(StringBuilder out, String label) {
                        out.append(label);
                    }
                }
                """);

        final var rows = recipe.collectedRows();
        assertAll(
                () -> assertEquals(1, rows.size()),
                () -> assertEquals(StringBuilderThreadingRecipe.Kind.THREADING, rows.getFirst().kind()),
                () -> assertEquals("renderRow", rows.getFirst().methodName()),
                () -> assertEquals("out", rows.getFirst().variableName()));
    }

    @Test
    void ignoresBuilderParameterThatIsNeverMutated() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int measure(StringBuilder candidate) {
                        return candidate.length();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresUnrelatedLocalVariablesAndParameters() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void work(List<String> items, int count) {
                        String sb = "not a builder";
                        int n = count;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void flagsBothPatternsInTheSameClass() {
        final var recipe = new StringBuilderThreadingRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String build() {
                        StringBuilder sb = new StringBuilder();
                        renderHeader(sb);
                        return sb.toString();
                    }
                    private void renderHeader(StringBuilder target) {
                        target.append("H");
                    }
                }
                """);

        assertEquals(2, recipe.collectedRows().size());
    }
}
