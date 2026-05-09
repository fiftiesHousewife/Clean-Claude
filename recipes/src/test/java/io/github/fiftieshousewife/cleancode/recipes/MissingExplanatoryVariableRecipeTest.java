package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingExplanatoryVariableRecipeTest {

    @Test
    void detectsDeeplyChainedMethodCallWithIntermediateArguments() {
        // The G19 smell is hidden complexity in an expression — typically
        // a stream-style pipeline whose intermediate steps deserve names.
        // A chain with arg-taking intermediate calls (filter, map, reduce)
        // qualifies because each step is computing something.
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.stream.Collectors;
                public class Foo {
                    List<String> bar;
                    void process() {
                        System.out.println(bar.stream().filter(s -> s.length() > 0).collect(Collectors.joining(",")));
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "stream().filter(...).collect(...) has intermediate arg-taking calls — fire");
    }

    @Test
    void ignoresPureGetterChainBecauseEachSegmentIsNavigationNotComputation() {
        // bar.getX().transform().serialize() and Gradle-style
        // reportFile.get().getAsFile().toPath() are deep but trivial:
        // every segment is a no-arg accessor / path step. Naming the
        // intermediates wouldn't add information; the chain reads as
        // a path expression. The G19 fix doesn't apply.
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getX().transform().serialize());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "no-arg navigation chain is not the G19 smell");
    }

    @Test
    void ignoresShortChainedMethodCallInArgument() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    void process() {
                        System.out.println(bar.getX().transform());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsComplexBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int compute(int a, int b, int c, int d, int e) {
                        return a + b * c - d + e;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("compute", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresChainedArgumentWhenOuterCallIsAlreadyExtractedToVariable() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object bar;
                    Object compute(Object x) { return x; }
                    void use(Object x) {}
                    void process() {
                        final var result = compute(bar.getX().transform().serialize());
                        use(result);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "compute(...) is already the initializer of a var decl; do not re-flag its chained argument");
    }

    @Test
    void ignoresChainArgumentWhereLambdaBodyIsSingleNegatedNamedCall() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class JulToSlf4jVisitor {
                    static boolean isJulLoggerFqn(final String typeName) {
                        return "java.util.logging.Logger".equals(typeName);
                    }
                    static List<String> wrap(final List<String> v) { return v; }
                    static List<String> withoutJulLoggerImport(final List<String> imports) {
                        return wrap(imports.stream()
                                .filter(imp -> !isJulLoggerFqn(imp))
                                .toList());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G19.1: a stream chain whose only argument-taking intermediate is "
                        + "`.filter(x -> !namedCall(...))` is already structurally explained — "
                        + "the lambda body is a single negated call to a named helper.");
    }

    @Test
    void ignoresChainArgumentWhereLambdaBodyIsMethodReference() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    static boolean isAllowed(final String s) { return !s.isEmpty(); }
                    static List<String> wrap(final List<String> v) { return v; }
                    static List<String> active(final List<String> items) {
                        return wrap(items.stream()
                                .filter(Foo::isAllowed)
                                .toList());
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G19.1: method-reference arguments are inherently named — "
                        + "the chain is not 'complex' just because of the .filter(Class::method) hop.");
    }

    @Test
    void stillFiresOnChainArgumentWithNonTrivialLambdaBody() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.stream.Collectors;
                public class Foo {
                    List<String> bar;
                    void process() {
                        System.out.println(bar.stream().filter(s -> s.length() > 0).collect(Collectors.joining(",")));
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "G19 still fires when a lambda body is a real expression (s.length() > 0) — "
                        + "single-call exclusion only applies to one-method-invocation lambda bodies.");
    }

    @Test
    void ignoresStringConcatReturnInsideGetterNamedMethod() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class ConcatThrowableMessage {
                    public String getDescription() {
                        return "Rewrites SLF4J log calls of the form `log.error(...)` "
                                + "into `log.error(...)`. Peels the trailing `+ e.getMessage()` "
                                + "off the message and passes the throwable as a separate argument "
                                + "so SLF4J can append the stack trace. Multi-part LHS chains are "
                                + "preserved verbatim.";
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G19.2: a multi-line `+`-joined string-literal return inside a `getDescription()` "
                        + "(or any `get[A-Z].*` method) is its own explanation — extracting to a "
                        + "private static final DESCRIPTION constant just adds indirection.");
    }

    @Test
    void stillFiresOnStringConcatReturnInNonGetter() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String render(int n) {
                        return "step 1 " + n + " step 2 " + n + " step 3 " + n + " step 4 " + n;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "G19 still fires on a string concat return when the method is not a getter.");
    }

    @Test
    void ignoresSimpleBinaryExpressionInReturn() {
        final var recipe = new MissingExplanatoryVariableRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    int compute(int a, int b, int c, int d) {
                        return a + b * c - d;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }
}
