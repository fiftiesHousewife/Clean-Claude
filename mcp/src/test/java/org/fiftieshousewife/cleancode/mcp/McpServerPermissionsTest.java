package org.fiftieshousewife.cleancode.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the project-scope {@code .claude/settings.json} pre-allows
 * every tool this server exposes. Without this, the first
 * {@code claude -p} subprocess to call an MCP tool hits a permission
 * prompt, and because the subprocess is non-interactive, it silently
 * falls back to Edit/Bash. Keeps the config and the tool registry in
 * lock-step — adding a new tool to {@link McpServer#defaultRegistry}
 * without updating the settings file fails this test.
 */
class McpServerPermissionsTest {

    private static final String SETTINGS_PATH = "../.claude/settings.json";
    private static final String PERMISSION_PREFIX = "mcp__" + "cleancode-refactoring__";

    @Test
    void everyServerToolIsAllowedInProjectSettings() throws IOException {
        final Set<String> allowed = loadAllowedPermissions();
        final List<String> toolNames = McpServer
                .defaultRegistry(new GradleInvoker(Path.of(".")))
                .toolNames();

        final java.util.List<String> missing = new java.util.ArrayList<>();
        for (final String tool : toolNames) {
            final String entry = PERMISSION_PREFIX + tool;
            if (!allowed.contains(entry)) {
                missing.add(entry);
            }
        }
        assertTrue(missing.isEmpty(),
                "missing allow entries in .claude/settings.json: " + missing);
    }

    @Test
    void sandboxPathsAllowedForWriteAndEdit() throws IOException {
        final Set<String> allowed = loadAllowedPermissions();
        assertAll(
                () -> assertTrue(allowed.stream().anyMatch(
                                p -> p.startsWith("Write(") && p.contains("/src/main/java/")),
                        "sandbox Write path must be allowed or the agent falls back to Edit"),
                () -> assertTrue(allowed.stream().anyMatch(p -> p.startsWith("Edit")),
                        "Edit must be allowed so the fallback path works"));
    }

    @Test
    void settingsFileDoesNotAllowDangerousBroadPaths() throws IOException {
        final Set<String> allowed = loadAllowedPermissions();
        assertAll(
                () -> assertFalse(allowed.contains("Write"),
                        "bare `Write` would allow the agent to write any file — keep it scoped"),
                () -> assertFalse(allowed.contains("Bash"),
                        "bare `Bash` is too broad; use command-scoped allows like `Bash(./gradlew *)`"));
    }

    private static Set<String> loadAllowedPermissions() throws IOException {
        final Path settings = Path.of(SETTINGS_PATH);
        if (!Files.exists(settings)) {
            return Set.of();
        }
        final JsonObject root = JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
        if (!root.has("permissions") || !root.getAsJsonObject("permissions").has("allow")) {
            return Set.of();
        }
        final JsonArray allow = root.getAsJsonObject("permissions").getAsJsonArray("allow");
        final Set<String> entries = new HashSet<>();
        allow.forEach(element -> entries.add(element.getAsString()));
        return entries;
    }
}
