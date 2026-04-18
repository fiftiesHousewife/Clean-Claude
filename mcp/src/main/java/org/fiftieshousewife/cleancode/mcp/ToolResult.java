package org.fiftieshousewife.cleancode.mcp;

/**
 * The payload a {@link Tool} returns when invoked. Matches the MCP
 * {@code tools/call} result shape: a single text block and an
 * {@code isError} flag. Structured errors (e.g. recipe rejections)
 * use {@code isError = true} with the reason in the text; transport
 * errors (e.g. bad arguments) also use this shape.
 */
public record ToolResult(String text, boolean isError) {

    public static ToolResult ok(final String text) {
        return new ToolResult(text, false);
    }

    public static ToolResult error(final String text) {
        return new ToolResult(text, true);
    }
}
