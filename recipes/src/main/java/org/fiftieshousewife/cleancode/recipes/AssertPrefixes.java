package org.fiftieshousewife.cleancode.recipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

final class AssertPrefixes {

    private static final String RESOURCE_PATH =
            "/org/fiftieshousewife/cleancode/recipes/assert-prefixes.txt";
    private static final String COMMENT_PREFIX = "#";

    private static final Set<String> PREFIXES = load();

    private AssertPrefixes() {}

    static boolean matches(final String methodName) {
        return PREFIXES.stream().anyMatch(methodName::startsWith);
    }

    private static Set<String> load() {
        final InputStream stream = AssertPrefixes.class.getResourceAsStream(RESOURCE_PATH);
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
