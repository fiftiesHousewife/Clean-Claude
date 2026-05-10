package io.github.fiftieshousewife.cleancode.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.FindingSource;
import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
import io.github.fiftieshousewife.cleancode.core.Severity;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Ben-Manes dependency-updates JSON reports and produces E1 findings
 * for outdated dependencies.
 */
public class DependencyUpdatesFindingSource implements FindingSource {

    private static final String TOOL = "dependency-updates";
    private static final String REPORT_FILE = "dependencyUpdates/report.json";
    private static final String VERSION_CATALOG = "gradle/libs.versions.toml";

    private static final Pattern CATALOG_MODULE = Pattern.compile(
            "module\\s*=\\s*\"([^\"]+):([^\":]+)\"");
    private static final Pattern CATALOG_GROUP_NAME = Pattern.compile(
            "group\\s*=\\s*\"([^\"]+)\"\\s*,\\s*name\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern CATALOG_NAME_GROUP = Pattern.compile(
            "name\\s*=\\s*\"([^\"]+)\"\\s*,\\s*group\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern CATALOG_SHORTHAND = Pattern.compile(
            "=\\s*\"([^\":]+):([^\":]+):[^\"]+\"");

    /**
     * Coordinates the Clean Code plugin pulls in transitively (Checkstyle, PMD,
     * SpotBugs, etc.). Consumers can't act on bumps to these without changes
     * inside the plugin itself, so we strip them from E1 findings when no
     * version catalog narrows the scope further.
     */
    private static final Set<String> CLEAN_CODE_INTERNAL_GROUPS = Set.of(
            "com.puppycrawl.tools",
            "net.sourceforge.pmd",
            "com.github.spotbugs",
            "com.github.spotbugs.snom",
            "com.google.errorprone",
            "de.aaschmid",
            "de.thetaphi",
            "com.diffplug.spotless",
            "com.gradleup.shadow",
            "com.vanniktech",
            "info.solidsoft.pitest");

    @Override
    public String id() {
        return TOOL;
    }

    @Override
    public String displayName() {
        return "Dependency Updates";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return Set.of(HeuristicCode.E1);
    }

    @Override
    public boolean isAvailable(final ProjectContext context) {
        return Files.exists(reportPath(context));
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return List.of();
        }

        final CatalogLocation location = locateCatalog(context.projectRoot());
        if (location == CatalogLocation.ANCESTOR) {
            return List.of();
        }

        final boolean hasCatalog = location == CatalogLocation.HERE;
        final Set<String> declaredCoordinates = hasCatalog
                ? readDeclaredCoordinates(context.projectRoot().resolve(VERSION_CATALOG))
                : Set.of();

        final JsonObject root = parseReport(report);
        final List<Finding> findings = new ArrayList<>();
        final Set<String> seenCoordinates = new LinkedHashSet<>();
        extractOutdated(root, findings, seenCoordinates, hasCatalog, declaredCoordinates);
        return findings;
    }

    private enum CatalogLocation { HERE, ANCESTOR, NONE }

    private CatalogLocation locateCatalog(final Path projectRoot) {
        if (Files.exists(projectRoot.resolve(VERSION_CATALOG))) {
            return CatalogLocation.HERE;
        }
        Path ancestor = projectRoot.getParent();
        while (ancestor != null) {
            if (Files.exists(ancestor.resolve(VERSION_CATALOG))) {
                return CatalogLocation.ANCESTOR;
            }
            if (Files.exists(ancestor.resolve("settings.gradle"))
                    || Files.exists(ancestor.resolve("settings.gradle.kts"))) {
                return CatalogLocation.NONE;
            }
            ancestor = ancestor.getParent();
        }
        return CatalogLocation.NONE;
    }

    private JsonObject parseReport(final Path report) throws FindingSourceException {
        try (Reader reader = Files.newBufferedReader(report)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FindingSourceException("Failed to read dependency updates report: " + report, e);
        }
    }

    private void extractOutdated(final JsonObject root, final List<Finding> findings,
                                 final Set<String> seenCoordinates, final boolean hasCatalog,
                                 final Set<String> declaredCoordinates) {
        final JsonObject outdated = root.getAsJsonObject("outdated");
        if (outdated == null) {
            return;
        }

        final JsonArray dependencies = outdated.getAsJsonArray("dependencies");
        if (dependencies == null) {
            return;
        }

        dependencies.forEach(dep ->
                extractDependency(dep.getAsJsonObject(), findings, seenCoordinates,
                        hasCatalog, declaredCoordinates));
    }

    private void extractDependency(final JsonObject dep, final List<Finding> findings,
                                   final Set<String> seenCoordinates, final boolean hasCatalog,
                                   final Set<String> declaredCoordinates) {
        final String group = dep.get("group").getAsString();
        final String name = dep.get("name").getAsString();
        final String coordinate = group + ":" + name;

        if (hasCatalog) {
            if (!declaredCoordinates.contains(coordinate)) {
                return;
            }
        } else if (CLEAN_CODE_INTERNAL_GROUPS.contains(group)) {
            return;
        }

        final String currentVersion = dep.get("version").getAsString();
        final String latestVersion = latestRelease(dep);

        if (latestVersion == null) {
            return;
        }

        if (!seenCoordinates.add(coordinate)) {
            return;
        }

        final String message = "Outdated dependency %s [%s -> %s]"
                .formatted(coordinate, currentVersion, latestVersion);
        findings.add(hasCatalog
                ? Finding.at(HeuristicCode.E1, VERSION_CATALOG, 0, 0,
                        message, Severity.ERROR, Confidence.HIGH, TOOL, coordinate)
                : Finding.projectLevel(HeuristicCode.E1,
                        message, Severity.ERROR, Confidence.HIGH, TOOL, coordinate));
    }

    private String latestRelease(final JsonObject dep) {
        final JsonObject available = dep.getAsJsonObject("available");
        if (available == null) {
            return null;
        }
        final JsonElement release = available.get("release");
        return release != null && !release.isJsonNull() ? release.getAsString() : null;
    }

    private Set<String> readDeclaredCoordinates(final Path catalog) {
        try {
            final String text = Files.readString(catalog);
            final Set<String> coordinates = new HashSet<>();
            collectCoordinates(text, CATALOG_MODULE, coordinates, 1, 2);
            collectCoordinates(text, CATALOG_GROUP_NAME, coordinates, 1, 2);
            collectCoordinates(text, CATALOG_NAME_GROUP, coordinates, 2, 1);
            collectCoordinates(text, CATALOG_SHORTHAND, coordinates, 1, 2);
            return Set.copyOf(coordinates);
        } catch (IOException e) {
            return Set.of();
        }
    }

    private static void collectCoordinates(final String text, final Pattern pattern,
                                           final Set<String> sink,
                                           final int groupIndex, final int nameIndex) {
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            sink.add(matcher.group(groupIndex) + ":" + matcher.group(nameIndex));
        }
    }

    private Path reportPath(final ProjectContext context) {
        return context.buildDir().resolve(REPORT_FILE);
    }
}
