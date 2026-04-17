package org.fiftieshousewife.cleancode.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UpdateVersionCatalogTask extends DefaultTask {

    private static final Pattern VERSION_REF_LINE =
            Pattern.compile("^([A-Za-z0-9_-]+)\\s*=\\s*\"([^\"]+)\"\\s*$");

    private static final Pattern LIBRARY_LINE =
            Pattern.compile("^([A-Za-z0-9_-]+)\\s*=\\s*\\{\\s*(.*)\\}\\s*$");

    private static final Pattern MODULE_FIELD =
            Pattern.compile("module\\s*=\\s*\"([^:\"]+):([^\"]+)\"");

    private static final Pattern GROUP_FIELD =
            Pattern.compile("group\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern NAME_FIELD =
            Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern VERSION_REF_FIELD =
            Pattern.compile("version\\.ref\\s*=\\s*\"([^\"]+)\"");

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

        final Map<String, String> coordinateToLatest = readOutdatedNonMajor(report);
        if (coordinateToLatest.isEmpty()) {
            getLogger().lifecycle("No non-major dependency updates to apply.");
            return;
        }

        final List<String> lines = Files.readAllLines(catalog);
        final Map<String, String> versionRefByCoordinate = indexLibraries(lines);
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

        if (versionRefToNewValue.isEmpty()) {
            getLogger().lifecycle("No catalog version.ref entries matched the outdated coordinates.");
            return;
        }

        final List<String> rewritten = rewriteVersions(lines, versionRefToNewValue);
        Files.write(catalog, rewritten);

        versionRefToNewValue.forEach((ref, newValue) ->
                getLogger().lifecycle("Bumped {} -> {}", ref, newValue));
    }

    private Map<String, String> readOutdatedNonMajor(final Path report) throws IOException {
        final Map<String, String> results = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(report)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            final JsonObject outdated = root.getAsJsonObject("outdated");
            if (outdated == null) {
                return results;
            }
            final JsonArray dependencies = outdated.getAsJsonArray("dependencies");
            if (dependencies == null) {
                return results;
            }
            for (final JsonElement depElement : dependencies) {
                final JsonObject dep = depElement.getAsJsonObject();
                final String group = dep.get("group").getAsString();
                final String name = dep.get("name").getAsString();
                final String current = dep.get("version").getAsString();
                final String latest = latestAvailable(dep);
                if (latest == null || isMajorBump(current, latest)) {
                    continue;
                }
                results.put(group + ":" + name, latest);
            }
        }
        return results;
    }

    private String latestAvailable(final JsonObject dep) {
        final JsonObject available = dep.getAsJsonObject("available");
        if (available == null) {
            return null;
        }
        for (final String key : List.of("milestone", "release", "integration")) {
            final JsonElement element = available.get(key);
            if (element != null && !element.isJsonNull()) {
                return element.getAsString();
            }
        }
        return null;
    }

    private boolean isMajorBump(final String current, final String latest) {
        return major(current) != major(latest);
    }

    private String major(final String version) {
        final int dot = version.indexOf('.');
        return dot < 0 ? version : version.substring(0, dot);
    }

    private Map<String, String> indexLibraries(final List<String> lines) {
        final Map<String, String> index = new LinkedHashMap<>();
        boolean inLibraries = false;
        for (final String raw : lines) {
            final String line = raw.trim();
            if (line.startsWith("[")) {
                inLibraries = "[libraries]".equals(line);
                continue;
            }
            if (!inLibraries || line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final Matcher matcher = LIBRARY_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            final String fields = matcher.group(2);
            final String coordinate = extractCoordinate(fields);
            final String versionRef = extractVersionRef(fields);
            if (coordinate != null && versionRef != null) {
                index.put(coordinate, versionRef);
            }
        }
        return index;
    }

    private String extractCoordinate(final String fields) {
        final Matcher module = MODULE_FIELD.matcher(fields);
        if (module.find()) {
            return module.group(1) + ":" + module.group(2);
        }
        final Matcher group = GROUP_FIELD.matcher(fields);
        final Matcher name = NAME_FIELD.matcher(fields);
        if (group.find() && name.find()) {
            return group.group(1) + ":" + name.group(1);
        }
        return null;
    }

    private String extractVersionRef(final String fields) {
        final Matcher matcher = VERSION_REF_FIELD.matcher(fields);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> rewriteVersions(final List<String> lines,
                                          final Map<String, String> versionRefToNewValue) {
        final List<String> out = new ArrayList<>(lines.size());
        boolean inVersions = false;
        for (final String raw : lines) {
            final String trimmed = raw.trim();
            if (trimmed.startsWith("[")) {
                inVersions = "[versions]".equals(trimmed);
                out.add(raw);
                continue;
            }
            if (!inVersions || trimmed.isEmpty() || trimmed.startsWith("#")) {
                out.add(raw);
                continue;
            }
            final Matcher matcher = VERSION_REF_LINE.matcher(trimmed);
            if (!matcher.matches()) {
                out.add(raw);
                continue;
            }
            final String ref = matcher.group(1);
            final String newValue = versionRefToNewValue.get(ref);
            if (newValue == null) {
                out.add(raw);
                continue;
            }
            final int leading = raw.length() - raw.stripLeading().length();
            final String indent = raw.substring(0, leading);
            out.add(indent + ref + " = \"" + newValue + "\"");
        }
        return out;
    }
}
