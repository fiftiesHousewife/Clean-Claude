package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.util.Map;

public record FeedbackRequest(String message, Map<String, String> context) {
    public FeedbackRequest {
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
