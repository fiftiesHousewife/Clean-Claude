package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractMethodRecipeTest {

    @Test
    void extractsTwoPrintlnsIntoVoidHelperWithTwoParameters() {
        final String source = """
                package com.example;
                public class Greeter {
                    public String greet() {
                        String heading = "Hello";
                        String trailing = "World";
                        System.out.println(heading);
                        System.out.println(trailing);
                        return heading + " " + trailing;
                    }
                }
                """;
        final String after = runRecipe(source, "Greeter.java", 6, 7, "logLines");
        assertAll(
                () -> assertTrue(after.contains("void logLines(String heading, String trailing)"),
                        "extracted signature has both read inputs, no return, in declaration order"),
                () -> assertTrue(after.contains("logLines(heading, trailing);"),
                        "call site replaces the range with the forwarded arguments"),
                () -> assertFalse(after.contains(
                                "System.out.println(heading);\n        "
                                        + "System.out.println(trailing);\n        return"),
                        "original println pair no longer lives inside greet()"));
    }

    @Test
    void extractsSumBlockIntoMethodReturningTheOutputLocal() {
        final String source = """
                package com.example;
                public class Calculator {
                    public int total(int a, int b) {
                        int doubled = a * 2;
                        int tripled = b * 3;
                        int sum = doubled + tripled;
                        return sum;
                    }
                }
                """;
        final String after = runRecipe(source, "Calculator.java", 4, 6, "computeSum");
        assertAll(
                () -> assertTrue(after.contains("int computeSum(int a, int b)"),
                        "signature: inputs a,b; return type int from the output local"),
                () -> assertTrue(after.contains("return sum;"),
                        "extracted method returns the output local"),
                () -> assertTrue(after.contains("int sum = computeSum(a, b);"),
                        "call site preserves the output local's declaration"),
                () -> assertTrue(after.contains("return sum;"),
                        "original return statement still present in total()"));
    }

    @Test
    void extractsAccumulatorLoopViaOuterLocalReassignment() {
        final String source = """
                package com.example;
                import java.util.List;
                public class Joiner {
                    public String build(List<String> items) {
                        String result = "";
                        for (String item : items) {
                            result = result + item;
                        }
                        return result;
                    }
                }
                """;
        final String after = runRecipe(source, "Joiner.java", 6, 8, "join");
        assertAll(
                () -> assertTrue(after.contains("String join("),
                        "extracted method returns the outer local's type"),
                () -> assertTrue(after.contains("return result;"),
                        "extracted method returns the updated outer local"),
                () -> assertTrue(after.contains("result = join("),
                        "call site re-assigns the existing local — no new declaration"),
                () -> assertFalse(after.contains("String result = join("),
                        "outer-write output must not redeclare the local"));
    }

    @Test
    void extractsGuardBlockUsingVoidConditionalExit() {
        final String source = """
                package com.example;
                public class Guarded {
                    public void process(String input) {
                        if (input == null) {
                            return;
                        }
                        System.out.println(input);
                    }
                }
                """;
        final String after = runRecipe(source, "Guarded.java", 4, 6, "isMissing");
        assertAll(
                () -> assertTrue(after.contains("boolean isMissing(String input)"),
                        "void conditional-exit uses boolean sentinel"),
                () -> assertTrue(after.contains("return true;"),
                        "bare return in the range is rewritten to `return true;`"),
                () -> assertTrue(after.contains("return false;"),
                        "a terminal `return false;` is appended to the extracted body"),
                () -> assertTrue(after.contains("if (isMissing(input)) return;"),
                        "call site wraps the invocation in `if (...) return;`"));
    }

    @Test
    void extractsLoopContainingInternalContinue() {
        final String source = """
                package com.example;
                import java.util.List;
                public class Skip {
                    public void process(List<String> rows) {
                        for (String row : rows) {
                            if (row == null) {
                                continue;
                            }
                            System.out.println(row.trim());
                        }
                    }
                }
                """;
        final String after = runRecipe(source, "Skip.java", 5, 10, "printRows");
        assertAll(
                () -> assertTrue(after.contains("void printRows("),
                        "Phase G: a continue that targets a loop inside the range is safe"),
                () -> assertTrue(after.contains("printRows(rows);"),
                        "call site forwards the outer local to the helper"));
    }

    @Test
    void extractsLoopContainingInternalBreak() {
        final String source = """
                package com.example;
                public class Iterating {
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            if (i > 5) {
                                break;
                            }
                        }
                    }
                }
                """;
        final String after = runRecipe(source, "Iterating.java", 4, 8, "loop");
        assertAll(
                () -> assertTrue(after.contains("void loop("),
                        "Phase G: a break that targets a loop already inside the range is safe"),
                () -> assertTrue(after.contains("loop();"),
                        "call site replaces the for-loop with the new helper invocation"));
    }

    @Test
    void rejectsRangeContainingReturn() {
        final String source = """
                package com.example;
                public class EarlyExit {
                    public int find(int[] xs, int target) {
                        for (int x : xs) {
                            if (x == target) {
                                return x;
                            }
                        }
                        return -1;
                    }
                }
                """;
        final var recipe = new ExtractMethodRecipe("EarlyExit.java", 4, 8, "scan");
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var results = recipe.run(
                new InMemoryLargeSourceSet(sourceFiles),
                new InMemoryExecutionContext(Throwable::printStackTrace));
        assertTrue(results.getChangeset().getAllResults().isEmpty(),
                "a range that contains `return` cannot be extracted without conditional-exit support");
    }

    @Test
    void rejectsMismatchedLineRange() {
        final String source = """
                package com.example;
                public class Tiny {
                    public void run() {
                        int x = 1;
                        int y = 2;
                    }
                }
                """;
        final var recipe = new ExtractMethodRecipe("Tiny.java", 99, 100, "nowhere");
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var results = recipe.run(
                new InMemoryLargeSourceSet(sourceFiles),
                new InMemoryExecutionContext(Throwable::printStackTrace));
        assertEquals(0, results.getChangeset().getAllResults().size(),
                "a range that lands outside any method body is a no-op");
    }

    private static String runRecipe(final String source, final String file,
                                    final int startLine, final int endLine,
                                    final String newMethodName) {
        final var recipe = new ExtractMethodRecipe(file, startLine, endLine, newMethodName);
        final List<SourceFile> sourceFiles = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final var ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final var results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx);
        final List<Result> changed = results.getChangeset().getAllResults();
        assertFalse(changed.isEmpty(), "recipe should have rewritten the compilation unit");
        return changed.getFirst().getAfter().printAll();
    }
}
