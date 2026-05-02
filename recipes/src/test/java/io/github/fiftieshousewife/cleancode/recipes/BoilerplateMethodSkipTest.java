package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BoilerplateMethodSkipTest {

    @Test
    void nullDensitySkipsEqualsBody() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String a;
                    private String b;
                    private String c;
                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        Foo other = (Foo) o;
                        return java.util.Objects.equals(a, other.a)
                            && java.util.Objects.equals(b, other.b)
                            && java.util.Objects.equals(c, other.c);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "equals(Object) is exempt — its null-check density is contractual, not a smell");
    }

    @Test
    void shortVariableNameSkipsEqualsParam() {
        final var recipe = new ShortVariableNameRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @Override
                    public boolean equals(Object o) {
                        return o == this;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "equals(Object) param naming is contractual; the recipe should not nag");
    }

    @Test
    void guardClauseSkipsEqualsBody() {
        final var recipe = new GuardClauseRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        return true;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "equals(Object) opens with the canonical guard pattern by contract");
    }

    @Test
    void hashCodeIsSkipped() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String a;
                    private String b;
                    private String c;
                    @Override
                    public int hashCode() {
                        return (a == null ? 0 : a.hashCode())
                            ^ (b == null ? 0 : b.hashCode())
                            ^ (c == null ? 0 : c.hashCode());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "hashCode() is exempt — its null-handling is contractual");
    }

    @Test
    void nonContractEqualsOverloadIsStillFlagged() {
        final var recipe = new NullDensityRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public boolean equals(String a, String b, String c) {
                        return a == null && b == null && c == null;
                    }
                }
                """);

        assertTrue(!recipe.collectedRows().isEmpty(),
                "a method named equals with a non-contract signature is just a regular method");
    }
}
