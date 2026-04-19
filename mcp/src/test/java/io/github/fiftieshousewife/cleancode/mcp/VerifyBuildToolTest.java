package io.github.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyBuildToolTest {

    @Test
    void successfulBuildCollapsesToBuildOk() {
        final GradleInvoker invoker = new GradleInvoker(
                (args, to) -> new GradleInvoker.Result(0, "> Task :sandbox:compileJava\nBUILD SUCCESSFUL"));

        final ToolResult result = new VerifyBuildTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertFalse(result.isError()),
                () -> assertEquals("build OK", result.text()));
    }

    @Test
    void failedBuildReturnsErrorAndFirstFewCompilerErrors() {
        final String gradleOutput = """
                > Task :sandbox:compileJava FAILED
                /path/Foo.java:10: error: cannot find symbol
                /path/Foo.java:20: error: ';' expected
                /path/Foo.java:30: error: class Foo is public, should be declared in a file named Foo.java
                BUILD FAILED in 2s""";
        final GradleInvoker invoker = new GradleInvoker(
                (args, to) -> new GradleInvoker.Result(1, gradleOutput));

        final ToolResult result = new VerifyBuildTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("build failed:")),
                () -> assertTrue(result.text().contains("cannot find symbol"),
                        "first compiler error line is preserved"),
                () -> assertTrue(result.text().contains("';' expected"),
                        "subsequent errors are preserved up to the limit"));
    }

    @Test
    void missingModuleArgumentReturnsBadArguments() {
        final GradleInvoker invoker = new GradleInvoker(
                (args, to) -> { throw new AssertionError("should not invoke gradle"); });

        final ToolResult result = new VerifyBuildTool(invoker).call(Map.of());

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("bad arguments")));
    }

    @Test
    void summaryTruncatesBeyondTenErrorLines() {
        final StringBuilder many = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            many.append("/path/Foo.java:").append(i).append(": error: something broken\n");
        }
        final String summary = VerifyBuildTool.summariseErrors(many.toString());
        assertTrue(summary.contains("further errors truncated"));
    }
}
