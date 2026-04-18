package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP adapter that runs {@code ./gradlew :<module>:test} and returns a
 * compact summary — `tests: all passed` on success, or `tests: N
 * failures — <first three lines>` on failure. Keeps the agent's
 * context free of Gradle's multi-page stack traces.
 */
public final class RunTestsTool implements Tool {

    private static final String NAME = "run_tests";
    private static final String MODULE = "module";
    private static final String TEST_CLASS = "testClass";
    private static final Pattern FAILED_LINE = Pattern.compile(
            "^(\\S+) > (\\S+)\\(.*\\) FAILED");
    private static final int FAILURE_LINE_LIMIT = 5;

    private final GradleInvoker gradle;

    public RunTestsTool(final GradleInvoker gradle) {
        this.gradle = gradle;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Run `./gradlew :<module>:test` (optionally filtered to a single test class) and "
                + "return a compact summary: `tests: all passed` on success, or the first few "
                + "failed test names on failure. Use this after every refactoring tool call "
                + "that could break a test.";
    }

    @Override
    public JsonObject inputSchema() {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        final JsonObject properties = new JsonObject();
        properties.add(MODULE, simpleProperty(
                "Gradle module name (e.g. `sandbox`, `refactoring`, `plugin`)."));
        properties.add(TEST_CLASS, simpleProperty(
                "Optional. Fully-qualified test class to run in isolation "
                        + "(e.g. `org.example.FooTest`)."));
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
        final Object testClass = arguments.get(TEST_CLASS);
        final List<String> args = new ArrayList<>();
        args.add(":" + module + ":test");
        if (testClass != null && !testClass.toString().isBlank()) {
            args.add("--tests");
            args.add(testClass.toString());
        }
        try {
            final GradleInvoker.Result result = gradle.invoke(args.toArray(new String[0]));
            return result.exitCode() == 0
                    ? ToolResult.ok("tests: all passed")
                    : ToolResult.error("tests failed:\n" + summariseFailures(result.output()));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ToolResult.error("gradle invocation failed: " + e.getMessage());
        }
    }

    static String summariseFailures(final String output) {
        final List<String> failures = new ArrayList<>();
        for (final String line : output.split("\\r?\\n")) {
            final Matcher match = FAILED_LINE.matcher(line.strip());
            if (match.find()) {
                failures.add("- " + match.group(1) + "." + match.group(2) + "()");
                if (failures.size() >= FAILURE_LINE_LIMIT) {
                    failures.add("(further failures truncated)");
                    break;
                }
            }
        }
        return failures.isEmpty() ? output.strip() : String.join("\n", failures);
    }

    private static JsonObject simpleProperty(final String description) {
        final JsonObject object = new JsonObject();
        object.addProperty("type", "string");
        object.addProperty("description", description);
        return object;
    }
}
