package org.fiftieshousewife.cleancode.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionCatalogIndex {

    private static final String LIBRARIES_SECTION_HEADER = "[libraries]";
    private static final String SECTION_HEADER_PREFIX = "[";
    private static final String COMMENT_PREFIX = "#";
    private static final int MODULE_GROUP_PART = 1;
    private static final int MODULE_ARTIFACT_PART = 2;

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
            if (isSectionHeader(line)) {
                inLibraries = LIBRARIES_SECTION_HEADER.equals(line);
                continue;
            }
            if (isSkippableLine(line, inLibraries)) {
                continue;
            }
            indexLibraryEntry(line, index);
        }
        return index;
    }

    private void indexLibraryEntry(final String line, final Map<String, String> index) {
        final Matcher matcher = LIBRARY_LINE.matcher(line);
        if (!matcher.matches()) {
            return;
        }
        final String fields = matcher.group(2);
        final String coordinate = extractCoordinate(fields);
        final String versionRef = extractVersionRef(fields);
        if (isValidEntry(coordinate, versionRef)) {
            index.put(coordinate, versionRef);
        }
    }

    private boolean isSectionHeader(final String line) {
        return line.startsWith(SECTION_HEADER_PREFIX);
    }

    private boolean isSkippableLine(final String line, final boolean inLibraries) {
        return !inLibraries || line.isEmpty() || line.startsWith(COMMENT_PREFIX);
    }

    private boolean isValidEntry(final String coordinate, final String versionRef) {
        return coordinate != null && versionRef != null;
    }

    String extractCoordinate(final String fields) {
        final Matcher module = MODULE_FIELD.matcher(fields);
        if (module.find()) {
            return module.group(MODULE_GROUP_PART) + ":" + module.group(MODULE_ARTIFACT_PART);
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
