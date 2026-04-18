package org.fiftieshousewife.cleancode.refactoring.movemethod;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveMethodRecipeTest {

    @Test
    void movesStaticMethodAndRewritesCallSites() {
        final String utils = """
                package com.example;
                public class Utils {
                    public static int doubleIt(int n) {
                        return n * 2;
                    }
                }
                """;
        final String client = """
                package com.example;
                public class Client {
                    public int compute(int x) {
                        return Utils.doubleIt(x);
                    }
                }
                """;
        final String helpers = """
                package com.example;
                public class Helpers {
                }
                """;
        final Map<String, String> after = runRecipe(
                List.of(utils, client, helpers),
                "Utils.java", "doubleIt", "com.example.Helpers");
        assertAll(
                () -> assertTrue(after.get("Helpers.java").contains("public static int doubleIt(int n)"),
                        "moved method lands on the target class"),
                () -> assertFalse(after.get("Utils.java").contains("doubleIt"),
                        "method is removed from the source class"),
                () -> assertTrue(after.get("Client.java").contains("Helpers.doubleIt(x)"),
                        "call site is retargeted at the new home"),
                () -> assertFalse(after.get("Client.java").contains("Utils.doubleIt"),
                        "no stale qualifier remains on the call site"));
    }

    @Test
    void rejectsInstanceMethod() {
        final String source = """
                package com.example;
                public class Widget {
                    public int area(int w, int h) {
                        return w * h;
                    }
                }
                """;
        final String target = """
                package com.example;
                public class Geometry {
                }
                """;
        final Map<String, String> after = runRecipe(
                List.of(source, target),
                "Widget.java", "area", "com.example.Geometry");
        assertAll(
                () -> assertTrue(after.get("Widget.java").contains("public int area"),
                        "instance method stays on its original class"),
                () -> assertFalse(after.get("Geometry.java").contains("area"),
                        "target class is untouched when the recipe rejects"));
    }

    @Test
    void rejectsMethodThatReferencesSiblingMembers() {
        final String source = """
                package com.example;
                public class Calc {
                    public static int TWO = 2;
                    public static int doubleIt(int n) {
                        return n * TWO;
                    }
                }
                """;
        final String target = """
                package com.example;
                public class Helpers {
                }
                """;
        final Map<String, String> after = runRecipe(
                List.of(source, target),
                "Calc.java", "doubleIt", "com.example.Helpers");
        assertTrue(after.get("Calc.java").contains("doubleIt"),
                "method that reads a sibling constant stays put — retargeting its body is out of scope for v1");
    }

    private static Map<String, String> runRecipe(final List<String> sources, final String file,
                                                 final String methodName, final String targetFqn) {
        final var recipe = new MoveMethodRecipe(file, methodName, targetFqn);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(sources.toArray(new String[0])).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        final List<Result> changed = results.getChangeset().getAllResults();
        final Map<String, String> afterByName = sourceFiles.stream()
                .collect(Collectors.toMap(sf -> fileName(sf.getSourcePath().toString()),
                        SourceFile::printAll));
        changed.forEach(r -> afterByName.put(
                fileName(r.getAfter().getSourcePath().toString()),
                r.getAfter().printAll()));
        return afterByName;
    }

    private static String fileName(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
