package io.github.fiftieshousewife.cleancode.plugin.serve;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServerTest {

    private static final Logger LOG = Logging.getLogger(ReportServerTest.class);

    private ReportServer server;
    private HttpClient client;

    @BeforeEach
    void start(@TempDir final Path tempDir) throws Exception {
        final Path report = tempDir.resolve("findings.html");
        Files.writeString(report, "<html><body>findings</body></html>");

        final ConfigSnapshot snapshot = new ConfigSnapshot(
                List.of("G36"),
                List.of("checkstyle:FinalLocalVariable"),
                Map.of("methodBlankLineSections", 6, "privateMethodMinLines", 12));

        server = ReportServer.start(0, () -> report, () -> snapshot,
                request -> ApplyChangesResponse.ok(request.changes().size()), LOG);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void rootServesTheReportHtml() throws Exception {
        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url())).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("findings")),
                () -> assertTrue(response.headers().firstValue("Content-Type")
                        .orElse("").startsWith("text/html")));
    }

    @Test
    void stateEndpointReturnsCurrentConfigSnapshot() throws Exception {
        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/state")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        final JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(json.has("disabledRecipes"),
                        "client UI needs disabled list to render toggle states"),
                () -> assertTrue(json.has("enabledOptionalRules")),
                () -> assertTrue(json.has("thresholds")),
                () -> assertEquals("G36", json.getAsJsonArray("disabledRecipes").get(0).getAsString()),
                () -> assertEquals(6, json.getAsJsonObject("thresholds")
                        .get("methodBlankLineSections").getAsInt()));
    }

    @Test
    void applyChangesAcceptsBatchAndReturnsAppliedCount() throws Exception {
        final String body = """
                {
                  "changes": [
                    {"kind":"disableRecipe","params":{"code":"G30"},"reason":"too many blank-line splits"},
                    {"kind":"suppressFinding","params":{"file":"Foo.java","line":"42","code":"G12"},"reason":"FQN required to disambiguate"}
                  ]
                }
                """;

        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/apply-changes"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        final JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertEquals(true, json.get("success").getAsBoolean()),
                () -> assertEquals(2, json.get("applied").getAsInt()));
    }

    @Test
    void applyChangesRejectsNonPost() throws Exception {
        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/apply-changes")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    void shutdownEndpointInvokesShutdownCallback(@TempDir final Path tempDir) throws Exception {
        if (server != null) {
            server.stop();
        }
        final Path report = tempDir.resolve("findings.html");
        Files.writeString(report, "<html></html>");
        final ConfigSnapshot snapshot = new ConfigSnapshot(List.of(), List.of(), Map.of());
        final CountDownLatch shutdownInvoked = new CountDownLatch(1);
        server = ReportServer.start(0, () -> report, () -> snapshot,
                request -> ApplyChangesResponse.ok(0),
                shutdownInvoked::countDown, LOG);

        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/shutdown"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(response.body().contains("stopping")),
                () -> assertTrue(shutdownInvoked.await(2, TimeUnit.SECONDS),
                        "shutdown callback must run after the response is sent"));
    }

    @Test
    void shutdownEndpointRejectsNonPost() throws Exception {
        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/shutdown")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    void feedbackEndpointPersistsViaHandler(@TempDir final Path tempDir) throws Exception {
        if (server != null) {
            server.stop();
        }
        final Path report = tempDir.resolve("findings.html");
        Files.writeString(report, "<html></html>");
        final ConfigSnapshot snapshot = new ConfigSnapshot(List.of(), List.of(), Map.of());
        final AtomicReference<FeedbackRequest> received = new AtomicReference<>();
        server = ReportServer.start(0, () -> report, () -> snapshot,
                request -> ApplyChangesResponse.ok(0),
                feedbackRequest -> {
                    received.set(feedbackRequest);
                    return FeedbackResponse.ok("/tmp/clean-code-feedback.md");
                },
                () -> { }, LOG);

        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/feedback"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"message\":\"please fix this\",\"context\":{\"code\":\"G18\"}}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        final JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(json.get("success").getAsBoolean()),
                () -> assertEquals("please fix this", received.get().message()),
                () -> assertEquals("G18", received.get().context().get("code")));
    }

    @Test
    void feedbackEndpointRejectsBlankMessage() throws Exception {
        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/feedback"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode(),
                "blank feedback messages must surface as 400 so the UI can show validation");
    }

    @Test
    void serverPropagatesApplyHandlerErrorAsBadRequest(@TempDir final Path tempDir) throws Exception {
        if (server != null) {
            server.stop();
        }
        final Path report = tempDir.resolve("findings.html");
        Files.writeString(report, "<html></html>");
        final ConfigSnapshot snapshot = new ConfigSnapshot(List.of(), List.of(), Map.of());
        final AtomicReference<ApplyChangesRequest> received = new AtomicReference<>();
        server = ReportServer.start(0, () -> report, () -> snapshot,
                request -> {
                    received.set(request);
                    return ApplyChangesResponse.failed(List.of("could not parse build.gradle.kts"));
                }, LOG);

        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(server.url() + "api/apply-changes"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"changes\":[{\"kind\":\"disableRecipe\",\"params\":{\"code\":\"G30\"},\"reason\":\"x\"}]}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        final JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
        assertAll(
                () -> assertEquals(400, response.statusCode(),
                        "failed apply must surface as 4xx so the client can show the error"),
                () -> assertEquals(false, json.get("success").getAsBoolean()),
                () -> assertEquals("could not parse build.gradle.kts",
                        json.getAsJsonArray("errors").get(0).getAsString()));
    }
}
