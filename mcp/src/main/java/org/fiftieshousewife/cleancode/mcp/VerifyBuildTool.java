package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * MCP adapter that runs {@code ./gradlew :<module>:compileJava} and
 * collapses Gradle's verbose output to a one-line summary — "build OK"
 * on success, the first few compiler error lines on failure. Agents
 * run this often enough that the cache-read saved per invocation
 * dominates the savings of the whole refactoring MCP server.
 */
public final class VerifyBuildTool implements Tool {

    private static final String NAME = "verify_build";
    private static final String MODULE = "module";
    private static final int ERROR_LINE_LIMIT = 10;

    private final GradleInvoker gradle;

    public VerifyBuildTool(final GradleInvoker gradle) {
        this.gradle = gradle;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Run `./gradlew :<module>:compileJava` and return a compact summary of the "
                + "result. On success returns `build OK`; on failure returns the first few "
                + "compiler error lines. Use this after every refactoring tool call that "
                + "changed Java source.";
    }

    @Override
    public JsonObject inputSchema() {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        final JsonObject properties = new JsonObject();
        final JsonObject moduleProp = new JsonObject();
        moduleProp.addProperty("type", "string");
        moduleProp.addProperty("description",
                "Gradle module name (e.g. `sandbox`, `refactoring`, `plugin`).");
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
            final GradleInvoker.Result result = gradle.invoke(":" + module + ":compileJava");
            return result.exitCode() == 0
                    ? ToolResult.ok("build OK")
                    : ToolResult.error("build failed:\n" + summariseErrors(result.output()));
        } catch (IOException e) {
            return ToolResult.error("gradle invocation failed: " + e.getMessage());
        }
    }

    static String summariseErrors(final String output) {
        final StringBuilder summary = new StringBuilder();
        int kept = 0;
        for (final String line : output.split("\\r?\\n")) {
            if (line.contains("error:") || line.contains("FAILED")) {
                summary.append(line.stripTrailing()).append('\n');
                if (++kept >= ERROR_LINE_LIMIT) {
                    summary.append("(further errors truncated)\n");
                    break;
                }
            }
        }
        return summary.length() == 0 ? output.strip() : summary.toString().stripTrailing();
    }
}
