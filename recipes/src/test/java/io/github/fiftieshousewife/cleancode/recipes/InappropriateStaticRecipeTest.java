package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InappropriateStaticRecipeTest {

    @Test
    void detectsMethodNotUsingInstanceState() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertAll(
                () -> assertEquals(1, recipe.collectedRows().size()),
                () -> assertEquals("Foo", recipe.collectedRows().getFirst().className()),
                () -> assertEquals("add", recipe.collectedRows().getFirst().methodName())
        );
    }

    @Test
    void ignoresMethodWithOverrideAnnotation() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    @Override
                    public String toString() {
                        String result = "Foo";
                        String padded = result + "!";
                        return padded;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresPrivateMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodAccessingInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private String name;
                    public String greet() {
                        String greeting = "Hello";
                        String full = greeting + " " + this.name;
                        return full;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodCallingInstanceMethod() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public String greet() {
                        String name = getName();
                        String greeting = "Hello " + name;
                        return greeting;
                    }
                    private String getName() { return "World"; }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresStaticMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public static int add(int a, int b) {
                        int sum = a + b;
                        int doubled = sum * 2;
                        return doubled;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresShortMethods() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresMethodWritingUnqualifiedInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class CsvParser {
                    private int rowsParsed;
                    public int parseRow(String row) {
                        int count = 0;
                        for (int i = 0; i < row.length(); i++) count++;
                        rowsParsed++;
                        return count;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "writing `rowsParsed++` without `this.` still counts as instance state");
    }

    @Test
    void ignoresMethodReadingUnqualifiedInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class SessionStore {
                    private final Map<String, String> sessions;
                    public SessionStore(Map<String, String> sessions) { this.sessions = sessions; }
                    public String lookupOrNull(String id) {
                        String normalised = id.trim();
                        String result = sessions.get(normalised);
                        return result;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "reading `sessions.get(...)` without `this.` still counts as instance state");
    }

    @Test
    void stillFlagsMethodUsingOnlyStaticFieldOfSameClass() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Calculator {
                    private static final int MULTIPLIER = 2;
                    public int doubled(int x) {
                        int step = x + 0;
                        int result = step * MULTIPLIER;
                        return result;
                    }
                }
                """);

        assertEquals(1, recipe.collectedRows().size(),
                "reading a static field of the same class does NOT count as instance state");
    }

    @Test
    void ignoresClassesImplementingInterfaces() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo implements Comparable<Foo> {
                    public int compareTo(Foo other) {
                        int a = 1;
                        int b = 2;
                        return a - b;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresParseRowShapeFromCsvParser() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.ArrayList;
                import java.util.List;
                public final class CsvParser {
                    private final char separator;
                    private final char quote;
                    private int rowsParsed = 0;
                    public CsvParser(final char separator, final char quote) {
                        this.separator = separator;
                        this.quote = quote;
                    }
                    public List<String> parseRow(final String line) {
                        final List<String> fields = new ArrayList<>();
                        final StringBuilder current = new StringBuilder();
                        int index = 0;
                        while (index < line.length()) {
                            final char ch = line.charAt(index);
                            if (ch == quote) {
                                index = index + 1;
                                continue;
                            }
                            if (ch == separator) {
                                fields.add(current.toString());
                                current.setLength(0);
                                index = index + 1;
                                continue;
                            }
                            current.append(ch);
                            index = index + 1;
                        }
                        fields.add(current.toString());
                        rowsParsed = rowsParsed + 1;
                        return fields;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "parseRow reads instance fields separator/quote and assigns rowsParsed — not G18-able");
    }

    @Test
    void ignoresExecuteShapeFromHttpRetryPolicy() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.concurrent.Callable;
                public final class HttpRetryPolicy {
                    private final StringBuilder audit = new StringBuilder();
                    public <T> T execute(final Callable<T> action) {
                        int attempt = 0;
                        while (attempt < 5) {
                            try {
                                final T result = action.call();
                                audit.append("attempt ").append(attempt).append(" ok\\n");
                                return result;
                            } catch (Exception e) {
                                audit.append("attempt failed: ").append(e.getMessage()).append('\\n');
                            }
                            attempt = attempt + 1;
                        }
                        return null;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "execute calls `audit.append(...)` on an instance field — not G18-able");
    }

    @Test
    void ignoresSessionOpenShapeWhichMutatesInstanceMapViaPut() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.HashMap;
                import java.util.Map;
                public final class SessionStore {
                    private final Map<String, String> sessions = new HashMap<>();
                    public void open(final String token, final String userId) {
                        if (token == null || token.isBlank()) {
                            throw new IllegalArgumentException("token is required");
                        }
                        if (userId == null || userId.isBlank()) {
                            throw new IllegalArgumentException("userId is required");
                        }
                        sessions.put(token, userId);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "open mutates instance field `sessions` via put() — not G18-able");
    }

    @Test
    void ignoresActiveSessionCountShapeWithEnhancedForOverInstanceMap() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.HashMap;
                import java.util.Map;
                public final class SessionStore {
                    private final Map<String, String> sessions = new HashMap<>();
                    public int activeSessionCount() {
                        int count = 0;
                        for (final String userId : sessions.values()) {
                            if (!userId.isBlank()) {
                                count = count + 1;
                            }
                        }
                        return count;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "activeSessionCount iterates instance field `sessions.values()` — not G18-able");
    }

    @Test
    void ignoresUnqualifiedCallToInheritedInstanceMethod() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import org.openrewrite.java.JavaIsoVisitor;
                import org.openrewrite.ExecutionContext;
                import org.openrewrite.java.tree.J;
                public class SystemOutVisitor extends JavaIsoVisitor<ExecutionContext> {
                    public J.MethodInvocation replacePrint(final J.MethodInvocation method, final boolean isError) {
                        final java.util.List<?> args = method.getArguments();
                        if (args.size() == 1) {
                            return handleSingleArgument(getCursor(), method, args.get(0), isError);
                        }
                        return method;
                    }
                    private J.MethodInvocation handleSingleArgument(Object cursor, J.MethodInvocation method,
                            Object arg, boolean isError) {
                        return method;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G18.1: getCursor() is an inherited instance method on JavaIsoVisitor — "
                        + "must count as instance state even when the parser can't resolve its type "
                        + "(no classpath context in the harness)");
    }

    @Test
    void ignoresMethodTargetedByMethodReferenceInAnotherCompilationUnit() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe,
                """
                package com.example;
                public class SystemOutVisitor {
                    public Object replacePrint(final Object method, final boolean isError) {
                        if (method == null) {
                            return null;
                        }
                        if (isError) {
                            return method;
                        }
                        return method;
                    }
                }
                """,
                """
                package com.example;
                public enum PrintMethod {
                    PRINT(SystemOutVisitor::replacePrint);
                    private final Replacer replacer;
                    PrintMethod(final Replacer r) { this.replacer = r; }
                    @FunctionalInterface
                    interface Replacer {
                        Object apply(SystemOutVisitor visitor, Object method, boolean isError);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "G18.2: replacePrint is the target of an unbound `SystemOutVisitor::replacePrint` "
                        + "method reference (different compilation unit). Making it static would "
                        + "change the SAM arity from (visitor, method, isError) to (method, isError) "
                        + "and break the binding.");
    }

    @Test
    void ignoresUnqualifiedContainsKeyReadOfInstanceField() {
        final var recipe = new InappropriateStaticRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.HashMap;
                import java.util.Map;
                public final class SessionStore {
                    private final Map<String, String> sessions = new HashMap<>();
                    public String lookupOrNull(final String token) {
                        if (!sessions.containsKey(token)) {
                            return null;
                        }
                        final String userId = sessions.get(token);
                        return userId;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "lookupOrNull reads `sessions.containsKey(...)` + `sessions.get(...)` — not G18-able");
    }
}
