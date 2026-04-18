package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * MCP adapter that runs {@code ./gradlew :<module>:spotlessApply} — the
 * single "housekeeping → formatting" pass the agent calls once at the
 * end of a rework session. Keeps formatting decoupled from individual
 * refactoring recipes so a chain of N extractions doesn't re-format N
 * times.
 */
public final class FormatTool implements Tool {

    private static final String NAME = "format";
    private static final String MODULE = "module";

    private final GradleInvoker gradle;

    public FormatTool(final GradleInvoker gradle) {
        this.gradle = gradle;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Run `./gradlew :<module>:spotlessApply` to normalise formatting across the "
                + "module's sources after a rework session. Call once at the end — individual "
                + "refactoring tools do not format, so a chain of extractions should be followed "
                + "by a single format call rather than formatting each intermediate edit.";
    }

    @Override
    public JsonObject inputSchema() {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        final JsonObject properties = new JsonObject();
        final JsonObject moduleProp = new JsonObject();
        moduleProp.addProperty("type", "string");
        moduleProp.addProperty("description", "Gradle module name (e.g. `sandbox`, `refactoring`).");
        properties.add(MODULE, moduleProp);
        schema.add("properties", properties);
        final JsonArray required = new JsonArray();
        required.add(MODULE);
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolResult call(final Map<String, Object> arguments) {
        final Object moduleArg = arguments.get(MODULE);
        if (moduleArg == null || moduleArg.toString().isBlank()) {
            return ToolResult.error("bad arguments: missing required argument `module`");
        }
        final String module = moduleArg.toString();
        try {
            final GradleInvoker.Result result = gradle.invoke(":" + module + ":spotlessApply");
            return result.exitCode() == 0
                    ? ToolResult.ok("format applied")
                    : ToolResult.error("spotlessApply failed:\n" + result.output().strip());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ToolResult.error("gradle invocation failed: " + e.getMessage());
        }
    }
}
