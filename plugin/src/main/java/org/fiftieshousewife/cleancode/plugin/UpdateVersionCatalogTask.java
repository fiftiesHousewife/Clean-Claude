package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class UpdateVersionCatalogTask extends DefaultTask {

    @TaskAction
    public void run() throws IOException {
        final Path rootDir = getProject().getRootProject().getProjectDir().toPath();
        final Path catalog = rootDir.resolve("gradle/libs.versions.toml");
        final Path report = rootDir.resolve("build/dependencyUpdates/report.json");

        if (!Files.exists(catalog)) {
            throw new GradleException("No version catalog at " + catalog);
        }
        if (!Files.exists(report)) {
            throw new GradleException("No Ben-Manes report at " + report
                    + ". Run :dependencyUpdates first.");
        }

        final Map<String, String> coordinateToLatest =
                new OutdatedDependencyReport(report).nonMajorUpdatesByCoordinate();
        if (coordinateToLatest.isEmpty()) {
            getLogger().lifecycle("No non-major dependency updates to apply.");
            return;
        }

        final List<String> lines = Files.readAllLines(catalog);
        final Map<String, String> versionRefByCoordinate =
                new VersionCatalogIndex().versionRefByCoordinate(lines);
        final Map<String, String> versionRefToNewValue =
                resolveVersionRefUpdates(coordinateToLatest, versionRefByCoordinate);

        if (versionRefToNewValue.isEmpty()) {
            getLogger().lifecycle("No catalog version.ref entries matched the outdated coordinates.");
            return;
        }

        final List<String> rewritten =
                new VersionCatalogRewriter().rewriteVersions(lines, versionRefToNewValue);
        Files.write(catalog, rewritten);

        versionRefToNewValue.forEach((ref, newValue) ->
                getLogger().lifecycle("Bumped {} -> {}", ref, newValue));
    }

    private Map<String, String> resolveVersionRefUpdates(final Map<String, String> coordinateToLatest,
                                                         final Map<String, String> versionRefByCoordinate) {
        final Map<String, String> versionRefToNewValue = new LinkedHashMap<>();
        coordinateToLatest.forEach((coord, latest) -> {
            final String ref = versionRefByCoordinate.get(coord);
            if (ref != null) {
                versionRefToNewValue.put(ref, latest);
            } else {
                getLogger().lifecycle(
                        "Skip {}: no version.ref entry in catalog. "
                                + "Inline or direct-version libraries must be bumped by hand.",
                        coord);
            }
        });
        return versionRefToNewValue;
    }
}
