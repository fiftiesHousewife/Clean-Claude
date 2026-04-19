package io.github.fiftieshousewife.cleancode.adapters;

import java.nio.file.Path;

public final class PathUtils {

    private PathUtils() {}

    public static String relativise(String absolutePath, Path projectRoot) {
        String root = projectRoot.toString();
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
