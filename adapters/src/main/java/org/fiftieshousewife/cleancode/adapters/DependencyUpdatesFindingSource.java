package org.fiftieshousewife.cleancode.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Parses Ben-Manes dependency-updates JSON reports and produces E1 findings
 * for outdated dependencies.
 */
public class DependencyUpdatesFindingSource implements FindingSource {

    private static final String TOOL = "dependency-updates";
    private static final String REPORT_FILE = "dependencyUpdates/report.json";

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
        return extractOutdated(parseReport(report));
    }

    private JsonObject parseReport(Path report) throws FindingSourceException {
        try (Reader reader = Files.newBufferedReader(report)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FindingSourceException("Failed to read dependency updates report: " + report, e);
        }
    }

    private List<Finding> extractOutdated(JsonObject root) {
        final JsonArray dependencies = outdatedDependencies(root);
        return StreamSupport.stream(dependencies.spliterator(), false)
                .map(dep -> extractDependency(dep.getAsJsonObject()))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    private JsonArray outdatedDependencies(JsonObject root) {
        final JsonObject outdated = root.getAsJsonObject("outdated");
        if (outdated == null) {
            return new JsonArray();
        }
        final JsonArray dependencies = outdated.getAsJsonArray("dependencies");
        return dependencies == null ? new JsonArray() : dependencies;
    }

    Finding extractDependency(JsonObject dep) {
        final String latestVersion = latestAvailable(dep);
        if (latestVersion == null) {
            return null;
        }
        final String group = dep.get("group").getAsString();
        final String name = dep.get("name").getAsString();
        final String currentVersion = dep.get("version").getAsString();
        final String coordinate = group + ":" + name;
        return Finding.projectLevel(
                HeuristicCode.E1,
                "Outdated dependency %s [%s -> %s]".formatted(coordinate, currentVersion, latestVersion),
                Severity.WARNING,
                Confidence.HIGH,
                TOOL,
                coordinate);
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
