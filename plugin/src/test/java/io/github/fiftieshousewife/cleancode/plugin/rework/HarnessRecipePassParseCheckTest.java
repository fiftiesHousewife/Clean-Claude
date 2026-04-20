package io.github.fiftieshousewife.cleancode.plugin.rework;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Whole-pipeline regression: every sample here is a snippet that the
 * deterministic recipe pass has previously broken. For each sample we
 * run the full {@link HarnessRecipePass} chain and then re-parse the
 * output via {@link JavaParser}. If the output isn't parseable Java,
 * the test fails and the offending input is printed so the operator
 * can zero in on which recipe introduced the regression.
 *
 * <p>Add a new entry here any time a recipe is caught producing
 * non-parseable output in the wild. A deterministic recipe is meant
 * to preserve syntactic validity by construction; failures in this
 * test indicate a recipe bug (whitespace handling, AST substitution
 * at the wrong level, missing context).
 */
class HarnessRecipePassParseCheckTest {

    @TempDir
    Path tempDir;

    @TestFactory
    Stream<DynamicTest> everyBreakagePatternProducesParseableOutput() {
        final Map<String, String> samples = buildSampleCorpus();
        return samples.entrySet().stream()
                .map(entry -> dynamicTest(entry.getKey(),
                        () -> assertPassProducesParseableJava(entry.getKey(), entry.getValue())));
    }

    private void assertPassProducesParseableJava(final String name, final String source) throws IOException {
        final Path file = tempDir.resolve(name.replace(' ', '_') + ".java");
        Files.writeString(file, source);
        HarnessRecipePass.apply(List.of(file));
        final String after = Files.readString(file);
        final List<SourceFile> reparsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(after)
                .toList();
        final boolean anyParseError = reparsed.stream().anyMatch(sf -> sf instanceof ParseError);
        assertFalse(anyParseError, () -> "recipe output did not re-parse for `" + name + "`.\n"
                + "=== ORIGINAL ===\n" + source + "\n=== AFTER ===\n" + after);
    }

    private static Map<String, String> buildSampleCorpus() {
        final Map<String, String> samples = new LinkedHashMap<>();

        samples.put("override method must not be made static", """
                package com.example;
                public class Child {
                    @Override
                    public String toString() {
                        return "x";
                    }
                }
                """);

        samples.put("junit test method must not be made static", """
                package com.example;
                import org.junit.jupiter.api.Test;
                class FooTest {
                    @Test
                    void passesTrivially() {
                        int x = 1 + 1;
                    }
                }
                """);

        samples.put("lambda parameter must not gain final", """
                package com.example;
                import java.util.List;
                public class L {
                    void go(List<String> xs) {
                        xs.forEach(x -> System.out.println(x));
                    }
                }
                """);

        samples.put("annotated parameter gets final with correct whitespace", """
                package com.example;
                public class A {
                    public A(@SuppressWarnings("unused") int value) {
                        int x = value;
                    }
                }
                """);

        samples.put("catch parameter must not gain final", """
                package com.example;
                public class C {
                    public void run() {
                        try {
                            System.out.println("hi");
                        } catch (RuntimeException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                """);

        samples.put("try-with-resources variable must not gain final", """
                package com.example;
                import java.io.StringReader;
                public class T {
                    int one(String s) throws Exception {
                        try (StringReader r = new StringReader(s)) {
                            return r.read();
                        }
                    }
                }
                """);

        samples.put("for-each loop variable must not gain final", """
                package com.example;
                import java.util.List;
                public class F {
                    int sum(List<Integer> xs) {
                        int total = 0;
                        for (int x : xs) {
                            total += x;
                        }
                        return total;
                    }
                }
                """);

        samples.put("chained appends with intermediate conditional argument", """
                package com.example;
                public class B {
                    public String render(int n) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("count=").append(n).append(n == 1 ? "" : "s");
                        sb.append(" done");
                        return sb.toString();
                    }
                }
                """);

        samples.put("method with no visibility modifier still compiles after static insertion", """
                package com.example;
                public class S {
                    Runnable task() {
                        return () -> {};
                    }
                }
                """);

        samples.put("test class with multiple lifecycle methods", """
                package com.example;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.AfterEach;
                class LifecycleTest {
                    @BeforeEach
                    void setUp() {
                        int x = 1;
                    }
                    @Test
                    void doesTheThing() {
                        int y = 2;
                    }
                    @AfterEach
                    void tearDown() {
                        int z = 3;
                    }
                }
                """);

        samples.put("inner record with static helper methods", """
                package com.example;
                public class Host {
                    private record Pair(int a, int b) {
                        Pair plus(Pair other) {
                            return new Pair(a + other.a, b + other.b);
                        }
                    }
                }
                """);

        samples.put("anonymous inner class with override method", """
                package com.example;
                public class Ann {
                    Runnable task() {
                        return new Runnable() {
                            @Override
                            public void run() {
                                int n = 1;
                            }
                        };
                    }
                }
                """);

        samples.put("method reference in foreach", """
                package com.example;
                import java.util.List;
                public class M {
                    void each(List<String> xs) {
                        xs.forEach(System.out::println);
                    }
                }
                """);

        samples.put("consecutive appends with a non-append statement between", """
                package com.example;
                public class NonAdj {
                    public String render(int n) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("a");
                        System.out.println("inline");
                        sb.append("b");
                        return sb.toString();
                    }
                }
                """);

        samples.put("generic method declaration stays untouched on static insertion", """
                package com.example;
                public class G {
                    <T> T identity(T x) {
                        return x;
                    }
                }
                """);

        return samples;
    }
}
