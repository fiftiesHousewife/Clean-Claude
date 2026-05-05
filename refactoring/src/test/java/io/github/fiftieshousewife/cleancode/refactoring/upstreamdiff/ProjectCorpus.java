package io.github.fiftieshousewife.cleancode.refactoring.upstreamdiff;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Locates and reads Java sources across the multi-module project so the
 * diff harness has a realistic corpus. Anchors to the project root by
 * walking up from {@code user.dir} (the module dir under Gradle test
 * execution) until {@code settings.gradle.kts} is found.
 */
public final class ProjectCorpus {

    private ProjectCorpus() {}

    public static Path projectRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null && !Files.exists(cursor.resolve("settings.gradle.kts"))) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalStateException(
                    "Could not locate project root from user.dir=" + System.getProperty("user.dir"));
        }
        return cursor;
    }

    public static Stream<RecipeDiffHarness.NamedSource> javaSourcesContaining(final String fragment) {
        try {
            return Files.walk(projectRoot())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !excluded(p))
                    .filter(p -> containsFragment(p, fragment))
                    .map(ProjectCorpus::read);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<RecipeDiffHarness.NamedSource> javaSources() {
        try {
            return Files.walk(projectRoot())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !excluded(p))
                    .map(ProjectCorpus::read);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<RecipeDiffHarness.NamedSource> curatedFixtures(final String subdir) {
        final Path fixturesRoot = projectRoot()
                .resolve("refactoring/src/test/resources/upstreamdiff")
                .resolve(subdir);
        if (!Files.isDirectory(fixturesRoot)) {
            throw new IllegalStateException("Curated fixture directory missing: " + fixturesRoot);
        }
        try {
            return Files.walk(fixturesRoot)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .map(ProjectCorpus::read);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final List<String> EXCLUDED_SEGMENTS = List.of(
            "/build/", "/.gradle/", "/.git/", "/build-logic/build/");

    private static boolean excluded(final Path file) {
        final String path = file.toString();
        return EXCLUDED_SEGMENTS.stream().anyMatch(path::contains);
    }

    private static boolean containsFragment(final Path file, final String fragment) {
        try {
            return Files.readString(file).contains(fragment);
        } catch (final IOException e) {
            return false;
        }
    }

    private static RecipeDiffHarness.NamedSource read(final Path file) {
        try {
            final Path root = projectRoot();
            final String name = root.relativize(file).toString();
            return new RecipeDiffHarness.NamedSource(name, Files.readString(file));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}