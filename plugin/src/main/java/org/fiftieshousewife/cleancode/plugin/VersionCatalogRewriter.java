package org.fiftieshousewife.cleancode.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionCatalogRewriter {

    private static final Pattern VERSION_REF_LINE =
            Pattern.compile("^([A-Za-z0-9_-]+)\\s*=\\s*\"([^\"]+)\"\\s*$");

    List<String> rewriteVersions(final List<String> lines,
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
