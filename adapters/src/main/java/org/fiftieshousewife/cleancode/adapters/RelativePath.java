package org.fiftieshousewife.cleancode.adapters;

import java.nio.file.Path;

public final class RelativePath {

    private RelativePath() {}

    public static String of(final String absolutePath, final Path projectRoot) {
        final String root = projectRoot.toString();
        if (absolutePath.startsWith(root)) {
            String relative = absolutePath.substring(root.length());
            if (relative.startsWith("/") || relative.startsWith("\\")) {
                relative = relative.substring(1);
            }
            return relative;
        }
        return absolutePath;
    }
}
