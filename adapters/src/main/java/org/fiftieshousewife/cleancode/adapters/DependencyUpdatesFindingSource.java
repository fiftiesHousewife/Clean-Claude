package org.fiftieshousewife.cleancode.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        final JsonObject root = parseReport(report);
        final List<Finding> findings = new ArrayList<>();
        extractOutdated(root, findings);
        return findings;
    }

    private JsonObject parseReport(Path report) throws FindingSourceException {
        try (Reader reader = Files.newBufferedReader(report)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FindingSourceException("Failed to read dependency updates report: " + report, e);
        }
    }

    private void extractOutdated(JsonObject root, List<Finding> findings) {
        final JsonObject outdated = root.getAsJsonObject("outdated");
        if (outdated == null) {
            return;
        }

        final JsonArray dependencies = outdated.getAsJsonArray("dependencies");
        if (dependencies == null) {
            return;
        }

        dependencies.forEach(dep -> extractDependency(dep.getAsJsonObject(), findings));
    }

    private void extractDependency(JsonObject dep, List<Finding> findings) {
        final String group = dep.get("group").getAsString();
        final String name = dep.get("name").getAsString();
        final String currentVersion = dep.get("version").getAsString();
        final String latestVersion = latestAvailable(dep);

        if (latestVersion == null) {
            return;
        }

        final String coordinate = group + ":" + name;
        findings.add(Finding.projectLevel(
                HeuristicCode.E1,
                "Outdated dependency %s [%s -> %s]".formatted(coordinate, currentVersion, latestVersion),
                Severity.INFO,
                Confidence.HIGH,
                TOOL,
                coordinate));
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
        return context.reportsDir().resolve(REPORT_FILE);
    }
}
