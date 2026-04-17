package org.fiftieshousewife.cleancode.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OutdatedDependencyReport {

    private static final List<String> AVAILABLE_CHANNEL_PREFERENCE =
            List.of("milestone", "release", "integration");

    private final Path reportPath;

    OutdatedDependencyReport(final Path reportPath) {
        this.reportPath = reportPath;
    }

    Map<String, String> nonMajorUpdatesByCoordinate() throws IOException {
        final Map<String, String> results = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(reportPath)) {
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
        for (final String key : AVAILABLE_CHANNEL_PREFERENCE) {
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
}
