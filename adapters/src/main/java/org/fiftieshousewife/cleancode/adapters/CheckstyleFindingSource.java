package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.Severity;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CheckstyleFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private enum XmlSeverity {
        ERROR("error", Severity.ERROR),
        WARNING("warning", Severity.WARNING),
        INFO("info", Severity.INFO);

        private final String xmlValue;
        private final Severity severity;

        XmlSeverity(final String xmlValue, final Severity severity) {
            this.xmlValue = xmlValue;
            this.severity = severity;
        }

        static Severity parseOrDefault(final String xmlValue, final Severity defaultSeverity) {
            return Arrays.stream(values())
                    .filter(entry -> entry.xmlValue.equals(xmlValue))
                    .map(entry -> entry.severity)
                    .findFirst()
                    .orElse(defaultSeverity);
        }
    }

    private static final String DOCS_BASE = "https://checkstyle.org/checks/";
    private static final String MAPPING_RESOURCE = "/checkstyle/rule-mapping.properties";
    private static final String CHECK_SUFFIX = "Check";
    private static final String TOOL_ID = "checkstyle";
    private static final String RULE_MAPPING_SEPARATOR = "\\|";
    private static final int RULE_MAPPING_FIELD_COUNT = 4;
    private static final int RULE_MAPPING_CODE_INDEX = 0;
    private static final int RULE_MAPPING_SEVERITY_INDEX = 1;
    private static final int RULE_MAPPING_CONFIDENCE_INDEX = 2;
    private static final int RULE_MAPPING_URL_INDEX = 3;
    private static final Map<String, RuleMapping> RULE_MAP = loadRuleMap();

    @Override
    public String id() {
        return TOOL_ID;
    }

    @Override
    public String displayName() {
        return "Checkstyle";
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
        return elementsOf(XmlReportParser.parse(report).getElementsByTagName("file"))
                .flatMap(file -> findingsFromFile(file, context))
                .toList();
    }

    private Stream<Finding> findingsFromFile(final Element fileElement, final ProjectContext context) {
        final String relativePath = PathUtils.relativise(fileElement.getAttribute("name"), context.projectRoot());
        return elementsOf(fileElement.getElementsByTagName("error"))
                .map(error -> toFinding(error, relativePath))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    Optional<Finding> toFinding(final Element error, final String relativePath) {
        final String checkName = extractCheckName(error.getAttribute("source"));
        final RuleMapping mapping = RULE_MAP.get(checkName);
        if (mapping == null) {
            return Optional.empty();
        }
        final int line = Integer.parseInt(error.getAttribute("line"));
        final Severity severity = xmlSeverityOrDefault(error.getAttribute("severity"), mapping.severity());
        return Optional.of(new Finding(
                mapping.code(), relativePath, line, line,
                error.getAttribute("message"), severity, mapping.confidence(),
                TOOL_ID, checkName, Map.of("ruleUrl", mapping.ruleUrl())));
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("checkstyle/main.xml");
    }

    String extractCheckName(final String sourceFqn) {
        final int lastDot = sourceFqn.lastIndexOf('.');
        final String simpleName = lastDot >= 0 ? sourceFqn.substring(lastDot + 1) : sourceFqn;
        if (simpleName.endsWith(CHECK_SUFFIX)) {
            return simpleName.substring(0, simpleName.length() - CHECK_SUFFIX.length());
        }
        return simpleName;
    }

    private Severity xmlSeverityOrDefault(final String xmlSeverity, final Severity defaultSeverity) {
        return XmlSeverity.parseOrDefault(xmlSeverity, defaultSeverity);
    }

    private static Stream<Element> elementsOf(final NodeList nodes) {
        return IntStream.range(0, nodes.getLength()).mapToObj(i -> (Element) nodes.item(i));
    }

    private static Map<String, RuleMapping> loadRuleMap() {
        final Properties properties = new Properties();
        try (InputStream stream = CheckstyleFindingSource.class.getResourceAsStream(MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load " + MAPPING_RESOURCE, e);
        }
        final Map<String, RuleMapping> map = new HashMap<>();
        properties.forEach((key, value) -> map.put(key.toString(), parseRuleMapping(value.toString())));
        return Collections.unmodifiableMap(map);
    }

    private static RuleMapping parseRuleMapping(final String value) {
        final String[] parts = value.split(RULE_MAPPING_SEPARATOR);
        if (parts.length != RULE_MAPPING_FIELD_COUNT) {
            throw new IllegalStateException("Invalid rule mapping format: " + value);
        }
        return new RuleMapping(
                HeuristicCode.valueOf(parts[RULE_MAPPING_CODE_INDEX].trim()),
                Severity.valueOf(parts[RULE_MAPPING_SEVERITY_INDEX].trim()),
                Confidence.valueOf(parts[RULE_MAPPING_CONFIDENCE_INDEX].trim()),
                DOCS_BASE + parts[RULE_MAPPING_URL_INDEX].trim());
    }
}
