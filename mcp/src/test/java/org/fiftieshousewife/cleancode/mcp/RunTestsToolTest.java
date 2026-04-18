package org.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunTestsToolTest {

    @Test
    void allPassedCollapsesToOneLine() {
        final GradleInvoker invoker = new GradleInvoker(Path.of("."),
                (cmd, dir, to) -> new GradleInvoker.Result(0, "BUILD SUCCESSFUL"));

        final ToolResult result = new RunTestsTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertFalse(result.isError()),
                () -> assertEquals("tests: all passed", result.text()));
    }

    @Test
    void failuresSummariseAsClassDotMethod() {
        final String gradleOutput = """
                > Task :sandbox:test FAILED

                ReportBuilderTest > totalCountMatchesInputSum() FAILED
                    org.opentest4j.AssertionFailedError at ReportBuilderTest.java:47

                ReportBuilderTest > emptyRowsProduceEmptySection() FAILED
                    org.opentest4j.AssertionFailedError at ReportBuilderTest.java:63

                5 tests completed, 2 failed
                BUILD FAILED""";
        final GradleInvoker invoker = new GradleInvoker(Path.of("."),
                (cmd, dir, to) -> new GradleInvoker.Result(1, gradleOutput));

        final ToolResult result = new RunTestsTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("ReportBuilderTest.totalCountMatchesInputSum()")),
                () -> assertTrue(result.text().contains("ReportBuilderTest.emptyRowsProduceEmptySection()")));
    }

    @Test
    void testClassArgumentForwardsToGradleFilter() {
        final java.util.concurrent.atomic.AtomicReference<List<String>> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        final GradleInvoker invoker = new GradleInvoker(Path.of("."), (cmd, dir, to) -> {
            captured.set(cmd);
            return new GradleInvoker.Result(0, "BUILD SUCCESSFUL");
        });

        new RunTestsTool(invoker).call(Map.of("module", "sandbox", "testClass", "org.example.FooTest"));

        assertAll(
                () -> assertTrue(captured.get().contains(":sandbox:test")),
                () -> assertTrue(captured.get().contains("--tests")),
                () -> assertTrue(captured.get().contains("org.example.FooTest")));
    }
}
