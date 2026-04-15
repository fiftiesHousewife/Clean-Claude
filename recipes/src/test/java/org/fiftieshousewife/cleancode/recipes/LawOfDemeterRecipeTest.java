package org.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LawOfDemeterRecipeTest {

    private static final String CLASS_A = """
            package com.example;
            public class A {
                public B getB() { return new B(); }
            }
            """;

    private static final String CLASS_B = """
            package com.example;
            public class B {
                public C getC() { return new C(); }
            }
            """;

    private static final String CLASS_C = """
            package com.example;
            public class C {
                public String getValue() { return ""; }
            }
            """;

    @Test
    void detectsChainThroughProjectTypes() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe,
                CLASS_A, CLASS_B, CLASS_C,
                """
                package com.example;
                public class Foo {
                    A a;
                    void deep() {
                        a.getB().getC().getValue();
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertTrue(recipe.collectedRows().getFirst().depth() >= 3)
        );
    }

    @Test
    void ignoresChainThroughJdkTypes() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    void process(Map<String, String> map) {
                        map.entrySet().iterator().next().getKey().trim();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresChainOfDepthTwo() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe,
                CLASS_A, CLASS_B,
                """
                package com.example;
                public class Foo {
                    A a;
                    void shallow() {
                        a.getB().getC();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresFluentStreamChains() {
        final var recipe = new LawOfDemeterRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void process(List<String> items) {
                        items.stream().filter(s -> !s.isEmpty()).map(String::trim).toList();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
