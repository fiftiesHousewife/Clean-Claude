package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.util.Map;

/**
 * One staged change submitted from the HTML report to the serve endpoint.
 *
 * <p>{@code kind} discriminates: {@code disableRecipe}, {@code tuneThreshold},
 * {@code suppressFinding}. {@code params} carries kind-specific fields:
 * code / threshold key / file / line. {@code reason} is required by the
 * client UI before staging is allowed.
 */
public record PendingChange(String kind, Map<String, String> params, String reason) {

    public PendingChange {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("change kind is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required for every staged change");
        }
        params = Map.copyOf(params);
    }

    public String requireParam(final String key) {
        final String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(kind + " change is missing required param: " + key);
        }
        return value;
    }
}
