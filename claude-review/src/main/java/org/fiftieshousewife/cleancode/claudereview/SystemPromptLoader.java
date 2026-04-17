package org.fiftieshousewife.cleancode.claudereview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SystemPromptLoader {

    private static final String RESOURCE_PATH = "/claude-review-system.txt";

    String load() {
        try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("System prompt resource not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system prompt", e);
        }
    }
}
