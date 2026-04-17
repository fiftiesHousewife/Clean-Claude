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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpotBugsFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String SB_BASE = "https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#";
    private static final String MAPPING_RESOURCE = "/spotbugs/rule-mapping.properties";
    private static final String SPOTBUGS = "spotbugs";

    private static final Map<String, RuleMapping> RULE_MAP = loadMappings();

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
        RULE_MAP.values().forEach(m -> codes.add(m.code()));
        return Collections.unmodifiableSet(codes);
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
        final Document doc = XmlReportParser.parse(report);
        return elementsOf(doc.getElementsByTagName("BugInstance"))
                .map(this::findingFor)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Stream<Element> elementsOf(final NodeList nodes) {
        return IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .map(node -> (Element) node);
    }

    private Optional<Finding> findingFor(final Element bug) {
        final String type = bug.getAttribute("type");
        final RuleMapping mapping = lookupMapping(bug.getAttribute("category"), type);
        if (mapping == null) {
            return Optional.empty();
        }
        return firstSourceLine(bug).map(sourceLine -> new Finding(
                mapping.code(),
                sourceLine.getAttribute("sourcepath"),
                Integer.parseInt(sourceLine.getAttribute("start")),
                Integer.parseInt(sourceLine.getAttribute("end")),
                messageFor(bug, type),
                mapping.severity(),
                mapping.confidence(),
                SPOTBUGS,
                type,
                Map.of("ruleUrl", mapping.ruleUrl())));
    }

    static RuleMapping lookupMapping(final String category, final String type) {
        final RuleMapping specific = RULE_MAP.get(category + "/" + type);
        return specific != null ? specific : RULE_MAP.get(category);
    }

    private static Optional<Element> firstSourceLine(final Element bug) {
        return elementsOf(bug.getElementsByTagName("SourceLine")).findFirst();
    }

    private static String messageFor(final Element bug, final String fallbackType) {
        final NodeList shortMessages = bug.getElementsByTagName("ShortMessage");
        if (shortMessages.getLength() == 0) {
            return fallbackType;
        }
        return shortMessages.item(0).getTextContent().trim();
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }

    private static Map<String, RuleMapping> loadMappings() {
        final Properties properties = new Properties();
        try (InputStream stream = SpotBugsFindingSource.class.getResourceAsStream(MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + MAPPING_RESOURCE, e);
        }
        final Map<String, RuleMapping> mappings = new TreeMap<>();
        properties.stringPropertyNames().forEach(key ->
                mappings.put(key, parseMapping(properties.getProperty(key))));
        return Collections.unmodifiableMap(mappings);
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
