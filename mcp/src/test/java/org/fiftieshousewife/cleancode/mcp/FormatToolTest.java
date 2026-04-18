package org.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatToolTest {

    @Test
    void successCollapsesToFormatApplied() {
        final GradleInvoker invoker = new GradleInvoker(Path.of("."),
                (cmd, dir, to) -> new GradleInvoker.Result(0, "BUILD SUCCESSFUL"));

        final ToolResult result = new FormatTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertFalse(result.isError()),
                () -> assertEquals("format applied", result.text()));
    }

    @Test
    void failureReturnsErrorWithOutput() {
        final GradleInvoker invoker = new GradleInvoker(Path.of("."),
                (cmd, dir, to) -> new GradleInvoker.Result(1, "spotlessApply could not find style"));

        final ToolResult result = new FormatTool(invoker).call(Map.of("module", "sandbox"));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("spotlessApply failed")));
    }
}
