package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class HtmlReportStyles {

    private static final String RESOURCE_PATH = "/org/fiftieshousewife/cleancode/core/html-report.css";
    private static final String CSS = loadCss();

    private HtmlReportStyles() {}

    public static String css() {
        return CSS;
    }

    private static String loadCss() {
        try (InputStream stream = HtmlReportStyles.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing CSS resource: " + RESOURCE_PATH);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to load CSS resource: " + RESOURCE_PATH, exception);
        }
    }
}
