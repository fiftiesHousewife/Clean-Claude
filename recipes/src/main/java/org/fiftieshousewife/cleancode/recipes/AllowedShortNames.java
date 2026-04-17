package org.fiftieshousewife.cleancode.recipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

final class AllowedShortNames {

    private static final String RESOURCE_PATH =
            "/org/fiftieshousewife/cleancode/recipes/allowed-short-names.txt";
    private static final String COMMENT_PREFIX = "#";

    private static final Set<String> NAMES = load();

    private AllowedShortNames() {}

    static boolean contains(final String variableName) {
        return NAMES.contains(variableName);
    }

    private static Set<String> load() {
        final InputStream stream = AllowedShortNames.class.getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            throw new IllegalStateException("Missing resource: " + RESOURCE_PATH);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith(COMMENT_PREFIX))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to load " + RESOURCE_PATH, e);
        }
    }
}
