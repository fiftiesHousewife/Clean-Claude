package io.github.fiftieshousewife.cleancode.plugin.serve;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Localhost-only HTTP server that serves the clean-code findings report
 * and accepts apply-changes batches from the in-page UI.
 *
 * <p>The server is bound to {@code 127.0.0.1} so it never accepts a
 * request from another machine — the report contains source paths and
 * the apply endpoint mutates files on disk.
 */
public final class ReportServer {

    private static final String JSON = "application/json; charset=utf-8";
    private static final String HTML = "text/html; charset=utf-8";

    private final HttpServer httpServer;

    private ReportServer(final HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    public static ReportServer start(final int port,
                                      final Supplier<Path> reportPathSupplier,
                                      final Supplier<ConfigSnapshot> stateSupplier,
                                      final Function<ApplyChangesRequest, ApplyChangesResponse> applyHandler,
                                      final Logger logger) throws IOException {
        return start(port, reportPathSupplier, stateSupplier, applyHandler, () -> { }, logger);
    }

    public static ReportServer start(final int port,
                                      final Supplier<Path> reportPathSupplier,
                                      final Supplier<ConfigSnapshot> stateSupplier,
                                      final Function<ApplyChangesRequest, ApplyChangesResponse> applyHandler,
                                      final Runnable shutdownCallback,
                                      final Logger logger) throws IOException {
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        final Gson gson = new Gson();

        httpServer.createContext("/", new ReportHandler(reportPathSupplier, logger));
        httpServer.createContext("/api/state", new StateHandler(stateSupplier, gson, logger));
        httpServer.createContext("/api/apply-changes", new ApplyChangesHandler(applyHandler, gson, logger));
        httpServer.createContext("/api/shutdown", new ShutdownHandler(shutdownCallback, logger));

        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
        return new ReportServer(httpServer);
    }

    public int port() {
        return httpServer.getAddress().getPort();
    }

    public String url() {
        return "http://localhost:" + port() + "/";
    }

    public void stop() {
        httpServer.stop(0);
    }

    private static void send(final HttpExchange exchange, final int status, final String contentType,
                              final byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] readBody(final HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return in.readAllBytes();
        }
    }

    private record ReportHandler(Supplier<Path> reportPathSupplier, Logger logger) implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            try {
                final Path report = reportPathSupplier.get();
                if (!Files.exists(report)) {
                    send(exchange, 404, HTML, "<h1>Report not found</h1>".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                send(exchange, 200, HTML, Files.readAllBytes(report));
            } catch (RuntimeException e) {
                logger.warn("ReportServer: failed to serve report", e);
                send(exchange, 500, HTML, ("<h1>500</h1><pre>" + e.getMessage() + "</pre>")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private record StateHandler(Supplier<ConfigSnapshot> stateSupplier, Gson gson, Logger logger)
            implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            try {
                final ConfigSnapshot snapshot = stateSupplier.get();
                send(exchange, 200, JSON, gson.toJson(snapshot).getBytes(StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                logger.warn("ReportServer: /api/state failed", e);
                send(exchange, 500, JSON,
                        ("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}")
                                .getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private record ShutdownHandler(Runnable shutdownCallback, Logger logger) implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, JSON, "{\"error\":\"POST required\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            send(exchange, 200, JSON, "{\"stopping\":true}".getBytes(StandardCharsets.UTF_8));
            new Thread(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                try {
                    shutdownCallback.run();
                } catch (RuntimeException e) {
                    logger.warn("ReportServer: shutdown callback threw", e);
                }
            }, "cleanCodeServe-shutdown-trigger").start();
        }
    }

    private record ApplyChangesHandler(Function<ApplyChangesRequest, ApplyChangesResponse> applyHandler,
                                         Gson gson, Logger logger) implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, JSON, "{\"error\":\"POST required\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            try {
                final String body = new String(readBody(exchange), StandardCharsets.UTF_8);
                final ApplyChangesRequest request = gson.fromJson(body, ApplyChangesRequest.class);
                final ApplyChangesResponse response = applyHandler.apply(request);
                final int status = response.success() ? 200 : 400;
                send(exchange, status, JSON, gson.toJson(response).getBytes(StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                logger.warn("ReportServer: /api/apply-changes failed", e);
                send(exchange, 500, JSON,
                        ("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}")
                                .getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
