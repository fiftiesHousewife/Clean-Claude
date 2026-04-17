package org.fiftieshousewife.cleancode.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionCatalogIndex {

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

    Map<String, String> versionRefByCoordinate(final List<String> lines) {
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
}
