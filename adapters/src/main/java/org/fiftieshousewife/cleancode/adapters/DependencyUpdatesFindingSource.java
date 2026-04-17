package org.fiftieshousewife.cleancode.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fiftieshousewife.cleancode.adapters.VersionCatalogLocator.CatalogLocation;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.Severity;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses Ben-Manes dependency-updates JSON reports and produces E1 findings
 * for outdated dependencies.
 */
// TODO: G5 — requires human review: the coveredCodes/isAvailable/collectFindings preamble
// is duplicated across every *FindingSource in this package. Extracting it affects the
// FindingSource contract and every sibling implementation, which is out of scope for a
// single-file fix.
public class DependencyUpdatesFindingSource implements FindingSource {

    private static final String TOOL = "dependency-updates";
    private static final String REPORT_FILE = "dependencyUpdates/report.json";
    private static final String VERSION_CATALOG = "gradle/libs.versions.toml";

    private final VersionCatalogLocator catalogLocator = new VersionCatalogLocator();

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
    public boolean isAvailable(ProjectContext context) {
        return Files.exists(reportPath(context));
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return List.of();
        }

        final CatalogLocation location = catalogLocator.locate(context.projectRoot());
        if (location == CatalogLocation.ANCESTOR) {
            return List.of();
        }

        final JsonObject root = parseReport(report);
        final List<Finding> findings = new ArrayList<>();
        final Set<String> seenCoordinates = new LinkedHashSet<>();
        extractOutdated(root, findings, seenCoordinates, location == CatalogLocation.HERE);
        return findings;
    }

    private JsonObject parseReport(Path report) throws FindingSourceException {
        try (Reader reader = Files.newBufferedReader(report)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FindingSourceException("Failed to read dependency updates report: " + report, e);
        }
    }

    private void extractOutdated(JsonObject root, List<Finding> findings,
                                 Set<String> seenCoordinates, boolean hasCatalog) {
        final JsonObject outdated = root.getAsJsonObject("outdated");
        if (outdated == null) {
            return;
        }

        final JsonArray dependencies = outdated.getAsJsonArray("dependencies");
        if (dependencies == null) {
            return;
        }

        dependencies.forEach(dep ->
                extractDependency(dep.getAsJsonObject(), findings, seenCoordinates, hasCatalog));
    }

    private void extractDependency(JsonObject dep, List<Finding> findings,
                                   Set<String> seenCoordinates, boolean hasCatalog) {
        final String group = dep.get("group").getAsString();
        final String name = dep.get("name").getAsString();
        final String currentVersion = dep.get("version").getAsString();
        final String latestVersion = latestAvailable(dep);

        if (latestVersion == null) {
            return;
        }

        final String coordinate = group + ":" + name;
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

    private String latestAvailable(JsonObject dep) {
        final JsonObject available = dep.getAsJsonObject("available");
        if (available == null) {
            return null;
        }
        return firstNonNull(available, "milestone", "release", "integration");
    }

    private String firstNonNull(JsonObject obj, String... keys) {
        for (final String key : keys) {
            final JsonElement element = obj.get(key);
            if (element != null && !element.isJsonNull()) {
                return element.getAsString();
            }
        }
        return null;
    }

    private Path reportPath(ProjectContext context) {
        return context.buildDir().resolve(REPORT_FILE);
    }
}
