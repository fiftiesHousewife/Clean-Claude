package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.Severity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class SpotBugsFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String SB_BASE = "https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#";
    private static final String MAPPING_RESOURCE = "/spotbugs/rule-mapping.properties";
    private static final String SPOTBUGS = "spotbugs";

    private static final Map<String, RuleMapping> TYPE_MAP;
    private static final Map<String, RuleMapping> CATEGORY_MAP;

    static {
        final Map<String, RuleMapping> typeEntries = new TreeMap<>();
        final Map<String, RuleMapping> categoryEntries = new TreeMap<>();
        loadMappings(typeEntries, categoryEntries);
        TYPE_MAP = Collections.unmodifiableMap(typeEntries);
        CATEGORY_MAP = Collections.unmodifiableMap(categoryEntries);
    }

    @Override
    public String id() {
        return SPOTBUGS;
    }

    @Override
    public String displayName() {
        return "SpotBugs";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        TYPE_MAP.values().forEach(m -> codes.add(m.code()));
        CATEGORY_MAP.values().forEach(m -> codes.add(m.code()));
        return Collections.unmodifiableSet(codes);
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

        final Document doc = XmlReportParser.parse(report);
        final NodeList bugInstances = doc.getElementsByTagName("BugInstance");
        final List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < bugInstances.getLength(); i++) {
            final Element bug = (Element) bugInstances.item(i);
            findingFor(bug).ifPresent(findings::add);
        }

        return findings;
    }

    private Optional<Finding> findingFor(final Element bug) {
        final String type = bug.getAttribute("type");
        final String category = bug.getAttribute("category");

        final RuleMapping mapping = lookupMapping(category, type);
        if (mapping == null) {
            return Optional.empty();
        }

        final Element sourceLine = firstSourceLine(bug);
        if (sourceLine == null) {
            return Optional.empty();
        }

        final int startLine = Integer.parseInt(sourceLine.getAttribute("start"));
        final int endLine = Integer.parseInt(sourceLine.getAttribute("end"));
        final String sourcePath = sourceLine.getAttribute("sourcepath");
        final String message = messageFor(bug, type);

        return Optional.of(new Finding(
                mapping.code(), sourcePath, startLine, endLine,
                message, mapping.severity(), mapping.confidence(),
                SPOTBUGS, type, Map.of("ruleUrl", mapping.ruleUrl())));
    }

    static RuleMapping lookupMapping(final String category, final String type) {
        final RuleMapping specific = TYPE_MAP.get(category + "/" + type);
        if (specific != null) {
            return specific;
        }
        return CATEGORY_MAP.get(category);
    }

    private static Element firstSourceLine(final Element bug) {
        final NodeList sourceLines = bug.getElementsByTagName("SourceLine");
        if (sourceLines.getLength() == 0) {
            return null;
        }
        return (Element) sourceLines.item(0);
    }

    private static String messageFor(final Element bug, final String fallbackType) {
        final NodeList shortMessages = bug.getElementsByTagName("ShortMessage");
        if (shortMessages.getLength() == 0) {
            return fallbackType;
        }
        return shortMessages.item(0).getTextContent().trim();
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }

    private static void loadMappings(final Map<String, RuleMapping> typeEntries,
                                     final Map<String, RuleMapping> categoryEntries) {
        final Properties properties = new Properties();
        try (InputStream stream = SpotBugsFindingSource.class.getResourceAsStream(MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + MAPPING_RESOURCE, e);
        }

        properties.stringPropertyNames().forEach(key -> {
            final RuleMapping mapping = parseMapping(properties.getProperty(key));
            if (key.contains("/")) {
                typeEntries.put(key, mapping);
            } else {
                categoryEntries.put(key, mapping);
            }
        });
    }

    private static RuleMapping parseMapping(final String value) {
        final String[] parts = value.split(",", -1);
        if (parts.length != 4) {
            throw new IllegalStateException("Expected 4 comma-separated fields but got: " + value);
        }
        final HeuristicCode code = HeuristicCode.valueOf(parts[0].trim());
        final Severity severity = Severity.valueOf(parts[1].trim());
        final Confidence confidence = Confidence.valueOf(parts[2].trim());
        final String urlSlug = parts[3].trim();
        return new RuleMapping(code, severity, confidence, SB_BASE + urlSlug);
    }
}
