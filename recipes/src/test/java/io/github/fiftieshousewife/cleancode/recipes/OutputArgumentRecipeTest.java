package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputArgumentRecipeTest {

    @Test
    void detectsMutatedCollectionParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void populate(List<String> results) {
                        results.add("item");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size());
        assertEquals("results", recipe.collectedRows().getFirst().paramName());
    }

    @Test
    void ignoresReadOnlyParameter() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    int countItems(List<String> items) {
                        return items.size();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateAccumulatorHelpers() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    private void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "Private accumulator helpers are an internal-plumbing pattern, "
                        + "not an API design issue — F2 is about contract surfaces");
    }

    @Test
    void ignoresStaticAccumulatorHelpers() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    static void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "Static helpers are typically internal builders/factories — "
                        + "the mutation contract is documented by the static modifier");
    }

    @Test
    void stillFlagsPublicMutatingApi() {
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void register(List<String> out, String value) {
                        out.add(value);
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "Public methods that mutate args are genuine API smells — F2 must still fire");
    }

    @Test
    void ignoresOverrideMethodsBecauseTheirSignatureIsContractDefined() {
        // Generalises the OpenRewrite visitor case: any @Override method
        // has a signature dictated by its supertype. The F2 fix is
        // "return the result instead of mutating the argument" — but
        // you can't change the signature of an override without
        // breaking the contract. Same applies to Consumer.accept(T),
        // BiConsumer.accept(T,U), custom Visitor.visitX(...), etc.
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.function.Consumer;
                public class Foo implements Consumer<List<String>> {
                    @Override
                    public void accept(List<String> out) {
                        out.add("item");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "@Override methods are signature-frozen — F2's fix doesn't apply");
    }

    @Test
    void stillDetectsNonOverrideMethodsWithIdenticalShape() {
        // Sanity check: it's the @Override that matters, not the
        // method's name or shape. A non-override `accept(List<...>)` is
        // still a real F2 candidate — the user CAN return the result.
        final var recipe = new OutputArgumentRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    public void accept(List<String> out) {
                        out.add("item");
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "non-override methods are not signature-frozen — F2 still applies");
    }
}
