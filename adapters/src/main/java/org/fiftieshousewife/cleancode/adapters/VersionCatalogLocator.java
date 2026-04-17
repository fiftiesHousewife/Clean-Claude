package org.fiftieshousewife.cleancode.adapters;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates a Gradle version catalog (gradle/libs.versions.toml) relative to a
 * project root, and reports whether it lives in this project, an ancestor
 * project (a parent multi-module build), or nowhere at all.
 */
final class VersionCatalogLocator {

    private static final String VERSION_CATALOG = "gradle/libs.versions.toml";
    private static final String SETTINGS_GROOVY = "settings.gradle";
    private static final String SETTINGS_KOTLIN = "settings.gradle.kts";

    enum CatalogLocation { HERE, ANCESTOR, NONE }

    CatalogLocation locate(final Path projectRoot) {
        if (Files.exists(projectRoot.resolve(VERSION_CATALOG))) {
            return CatalogLocation.HERE;
        }
        Path ancestor = projectRoot.getParent();
        while (ancestor != null) {
            if (Files.exists(ancestor.resolve(VERSION_CATALOG))) {
                return CatalogLocation.ANCESTOR;
            }
            if (isSettingsRoot(ancestor)) {
                return CatalogLocation.NONE;
            }
            ancestor = ancestor.getParent();
        }
        return CatalogLocation.NONE;
    }

    private boolean isSettingsRoot(final Path dir) {
        return Files.exists(dir.resolve(SETTINGS_GROOVY))
                || Files.exists(dir.resolve(SETTINGS_KOTLIN));
    }
}
