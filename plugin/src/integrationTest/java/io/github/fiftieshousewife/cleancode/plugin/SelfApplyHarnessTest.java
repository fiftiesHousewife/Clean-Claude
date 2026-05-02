package io.github.fiftieshousewife.cleancode.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self-apply harness: a fixture project mounts the plugin's own sources
 * via {@code srcDirs(...)} and runs the plugin against itself, mirroring
 * the manual sandbox at {@code /tmp/cleanclaude-selfanalysis-*}.
 *
 * <p>Catches regressions where the plugin compiles and unit-tests pass
 * but the report rendering, recipe wiring, or staging-bar markup breaks
 * when actually pointed at real source code.
 */
class SelfApplyHarnessTest {

    private static final List<String> MOUNTED_MODULES = List.of(
            "plugin", "core", "adapters", "recipes", "refactoring",
            "annotations", "claude-review");

    @TempDir
    Path projectDir;

    private Path repoRoot;

    @BeforeEach
    void setUp() throws IOException {
        final String repoRootProperty = System.getProperty("cleancode.repoRoot");
        assertTrue(repoRootProperty != null && !repoRootProperty.isBlank(),
                "integrationTest task must set -Dcleancode.repoRoot=...");
        repoRoot = Path.of(repoRootProperty);

        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"cleancode-self-apply\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), buildScript());
        Files.writeString(projectDir.resolve("CLAUDE.md"), "");
        Files.writeString(projectDir.resolve("gradle.properties"),
                "org.gradle.jvmargs=-Xss8m -Xmx2g\n");
    }

    @Test
    void analyseCleanCodeProducesReportFromOurOwnSources() throws IOException {
        final BuildResult result = runner("analyseCleanCode", "--stacktrace").build();

        final Path findingsJson = projectDir.resolve("build/reports/clean-code/findings.json");
        final Path findingsHtml = projectDir.resolve("build/reports/clean-code/findings.html");
        assertAll(
                () -> assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"),
                        "harness must complete cleanly with failOnViolation=false"),
                () -> assertTrue(Files.exists(findingsJson), "findings.json should be written"),
                () -> assertTrue(Files.exists(findingsHtml), "findings.html should be written"));

        final JsonObject json = new Gson().fromJson(Files.readString(findingsJson), JsonObject.class);
        final var findings = json.getAsJsonArray("findings");
        assertFalse(findings.isEmpty(),
                "self-apply must surface findings — empty result indicates broken recipe wiring");

        final Set<String> tools = new HashSet<>();
        findings.forEach(el -> tools.add(el.getAsJsonObject().get("tool").getAsString()));
        assertAll(
                () -> assertTrue(tools.contains("openrewrite"),
                        "openrewrite recipes must contribute findings; tools=" + tools),
                () -> assertTrue(tools.contains("checkstyle"),
                        "checkstyle adapter must contribute findings; tools=" + tools));
    }

    @Test
    void cleanCodeServeAcceptsBatchAndShutsDownViaApi() throws Exception {
        final int port = pickFreePort();
        Files.writeString(projectDir.resolve("build.gradle.kts"),
                buildScript() + "\ncleanCode { servePort.set(" + port + ") }\n");

        final AtomicReference<Throwable> serveError = new AtomicReference<>();
        final AtomicReference<BuildResult> serveResult = new AtomicReference<>();
        final Thread serveThread = new Thread(() -> {
            try {
                serveResult.set(runner("cleanCodeServe", "--stacktrace").build());
            } catch (Throwable t) {
                serveError.set(t);
            }
        }, "cleanCodeServe-runner");
        serveThread.setDaemon(true);
        serveThread.start();

        try {
            waitForPort(port, Duration.ofMinutes(2));

            final Path fixtureBuildScript = projectDir.resolve("build.gradle.kts");
            assertFalse(Files.readString(fixtureBuildScript).contains("\"G36\""),
                    "precondition: G36 must not be disabled before the apply call");

            final HttpClient client = HttpClient.newHttpClient();
            final String applyBody = "{\"changes\":[{\"kind\":\"disableRecipe\","
                    + "\"params\":{\"code\":\"G36\"},"
                    + "\"reason\":\"integration-test verifying staging->apply flow\"}]}";
            final HttpResponse<String> applyResponse = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/apply-changes"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(applyBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            final JsonObject applyJson = new Gson().fromJson(applyResponse.body(), JsonObject.class);
            assertAll("apply-changes response",
                    () -> assertEquals(200, applyResponse.statusCode(),
                            "apply must return 200 on success: body=" + applyResponse.body()),
                    () -> assertTrue(applyJson.get("success").getAsBoolean(),
                            "applyResponse.success=true"),
                    () -> assertEquals(1, applyJson.get("applied").getAsInt(),
                            "exactly one change applied"));

            assertTrue(Files.readString(fixtureBuildScript).contains("\"G36\""),
                    "build.gradle.kts must contain disableRecipe G36 after apply");

            final HttpResponse<String> shutdownResponse = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/shutdown"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, shutdownResponse.statusCode(),
                    "/api/shutdown must ack 200 before tearing down");

            serveThread.join(Duration.ofSeconds(30).toMillis());
            assertFalse(serveThread.isAlive(),
                    "cleanCodeServe must exit within 30s of /api/shutdown");

            if (serveError.get() != null) {
                throw new AssertionError("cleanCodeServe runner failed", serveError.get());
            }
            assertNotNull(serveResult.get(), "expected a BuildResult from cleanCodeServe");
            assertTrue(serveResult.get().getOutput().contains("BUILD SUCCESSFUL"),
                    "shutdown via /api/shutdown should produce a clean BUILD SUCCESSFUL");

            final Path pidFile = projectDir.resolve("build/clean-code/serve.pid");
            assertFalse(Files.exists(pidFile),
                    "serve.pid must be deleted on shutdown");
        } finally {
            if (serveThread.isAlive()) {
                serveThread.interrupt();
            }
        }
    }

    private static int pickFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForPort(final int port, final Duration timeout) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket("127.0.0.1", port)) {
                if (socket.isConnected()) {
                    return;
                }
            } catch (IOException retry) {
                Thread.sleep(500);
            }
        }
        throw new AssertionError("server did not bind to port " + port + " within " + timeout);
    }

    private GradleRunner runner(final String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput();
    }

    private String buildScript() {
        final StringBuilder script = new StringBuilder();
        script.append("plugins {\n");
        script.append("    `java-library`\n");
        script.append("    id(\"io.github.fiftieshousewife.cleancode\")\n");
        script.append("}\n\n");
        script.append("repositories {\n");
        script.append("    mavenCentral()\n");
        script.append("    gradlePluginPortal()\n");
        script.append("}\n\n");
        script.append("sourceSets {\n");
        script.append("    main {\n");
        script.append("        java {\n");
        script.append("            srcDirs(\n");
        for (int i = 0; i < MOUNTED_MODULES.size(); i++) {
            final String module = MOUNTED_MODULES.get(i);
            final String path = repoRoot.resolve(module).resolve("src/main/java")
                    .toAbsolutePath().toString().replace("\\", "\\\\");
            script.append("                \"").append(path).append("\"");
            if (i < MOUNTED_MODULES.size() - 1) {
                script.append(",");
            }
            script.append("\n");
        }
        script.append("            )\n");
        script.append("        }\n");
        script.append("    }\n");
        script.append("}\n\n");
        script.append("cleanCode {\n");
        script.append("    failOnViolation.set(false)\n");
        script.append("}\n\n");
        script.append("dependencies {\n");
        script.append("    compileOnly(\"org.openrewrite:rewrite-core:8.79.5\")\n");
        script.append("    compileOnly(\"org.openrewrite:rewrite-java:8.79.5\")\n");
        script.append("    compileOnly(\"com.google.code.gson:gson:2.11.0\")\n");
        script.append("    compileOnly(\"com.diffplug.spotless:spotless-plugin-gradle:6.25.0\")\n");
        script.append("    compileOnly(\"com.github.javaparser:javaparser-core:3.26.2\")\n");
        script.append("    compileOnly(\"com.anthropic:anthropic-java:2.25.0\")\n");
        script.append("}\n\n");
        script.append("tasks.named(\"compileJava\") { enabled = false }\n");
        script.append("tasks.named(\"classes\") { enabled = false }\n");
        // PMD's worker JVM in TestKit's isolated daemon hits StackOverflow
        // on this many sources; manual harness invocations fork a normal
        // daemon and work fine. Skip pmdMain here — coverage of the PMD
        // adapter is provided by CpdIntegrationTest + unit tests.
        script.append("tasks.named(\"pmdMain\") { enabled = false }\n");
        return script.toString();
    }
}
