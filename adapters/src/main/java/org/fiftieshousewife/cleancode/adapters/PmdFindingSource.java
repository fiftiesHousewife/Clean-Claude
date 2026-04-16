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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PmdFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String RULE_MAPPING_RESOURCE = "/pmd/pmd-rule-mapping.properties";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final int FIELD_COUNT = 5;

    private static final Map<String, String> CATEGORY_URLS = Map.of(
            "BP", "https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#",
            "DS", "https://pmd.github.io/pmd/pmd_rules_java_design.html#",
            "EP", "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#",
            "CS", "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#");

    private static final Map<String, RuleMapping> RULE_MAP = loadRuleMap();

    @Override
    public String id() {
        return "pmd";
    }

    @Override
    public String displayName() {
        return "PMD";
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

        try {
            final Document doc = XmlReportParser.parse(report);
            final List<Finding> findings = new ArrayList<>();
            elementsOf(doc.getElementsByTagName("file"))
                    .forEach(fileElement -> collectFromFile(fileElement, context, findings));
            return findings;
        } catch (final Exception e) {
            throw new FindingSourceException("Failed to parse PMD report: " + report, e);
        }
    }

    private void collectFromFile(final Element fileElement, final ProjectContext context,
                                 final List<Finding> findings) {
        final String absolutePath = fileElement.getAttribute("name");
        final String relativePath = PathUtils.relativise(absolutePath, context.projectRoot());
        elementsOf(fileElement.getElementsByTagName("violation"))
                .map(violation -> toFinding(violation, relativePath))
                .filter(finding -> finding != null)
                .forEach(findings::add);
    }

    private Finding toFinding(final Element violation, final String relativePath) {
        final String rule = violation.getAttribute("rule");
        final RuleMapping mapping = RULE_MAP.get(rule);
        if (mapping == null) {
            return null;
        }
        final int startLine = Integer.parseInt(violation.getAttribute("beginline"));
        final int endLine = Integer.parseInt(violation.getAttribute("endline"));
        final String message = violation.getTextContent().trim();
        return new Finding(
                mapping.code(), relativePath, startLine, endLine,
                message, mapping.severity(), mapping.confidence(),
                "pmd", rule, Map.of("ruleUrl", mapping.ruleUrl()));
    }

    private static Stream<Element> elementsOf(final NodeList nodes) {
        return IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .map(node -> (Element) node);
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("pmd/main.xml");
    }

    private static Map<String, RuleMapping> loadRuleMap() {
        final Properties properties = new Properties();
        try (final InputStream stream = PmdFindingSource.class.getResourceAsStream(RULE_MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("PMD rule mapping resource not found: " + RULE_MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load PMD rule mapping: " + RULE_MAPPING_RESOURCE, e);
        }
        final Map<String, RuleMapping> mappings = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(rule ->
                mappings.put(rule, parseMapping(rule, properties.getProperty(rule))));
        return Collections.unmodifiableMap(mappings);
    }

    private static RuleMapping parseMapping(final String rule, final String value) {
        final String[] parts = value.split(FIELD_SEPARATOR);
        if (parts.length != FIELD_COUNT) {
            throw new IllegalStateException("Invalid PMD rule mapping for " + rule + ": " + value);
        }
        final HeuristicCode code = HeuristicCode.valueOf(parts[0]);
        final Severity severity = Severity.valueOf(parts[1]);
        final Confidence confidence = Confidence.valueOf(parts[2]);
        final String categoryPrefix = CATEGORY_URLS.get(parts[3]);
        if (categoryPrefix == null) {
            throw new IllegalStateException("Unknown PMD rule category for " + rule + ": " + parts[3]);
        }
        return new RuleMapping(code, severity, confidence, categoryPrefix + parts[4]);
    }

}
