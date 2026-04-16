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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class CheckstyleFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String CHECKSTYLE_DOCS_BASE = "https://checkstyle.org/checks/";
    private static final String RULE_MAPPING_RESOURCE = "/checkstyle/rule-mapping.properties";
    private static final String CHECK_SUFFIX = "Check";
    private static final String TOOL_ID = "checkstyle";
    private static final String DISPLAY_NAME = "Checkstyle";
    private static final String REPORT_PATH = "checkstyle/main.xml";
    private static final String RULE_URL_KEY = "ruleUrl";
    private static final String FIELD_SEPARATOR = "\\|";

    private static final Map<String, RuleMapping> RULE_MAP = loadRuleMap();

    @Override
    public String id() {
        return TOOL_ID;
    }

    @Override
    public String displayName() {
        return DISPLAY_NAME;
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        RULE_MAP.values().forEach(m -> codes.add(m.code()));
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

        try {
            final Document doc = XmlReportParser.parse(report);
            final List<Finding> findings = new ArrayList<>();
            forEachFile(doc, (fileElement, relativePath) ->
                    collectFileFindings(fileElement, relativePath, context, findings));
            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse Checkstyle report: " + report, e);
        }
    }

    private void forEachFile(Document doc, FileVisitor visitor) {
        final NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            final Element fileElement = (Element) fileNodes.item(i);
            visitor.visit(fileElement, fileElement.getAttribute("name"));
        }
    }

    private void collectFileFindings(
            Element fileElement, String absolutePath, ProjectContext context, List<Finding> findings) {
        final String relativePath = PathUtils.relativise(absolutePath, context.projectRoot());
        final NodeList errors = fileElement.getElementsByTagName("error");
        for (int j = 0; j < errors.getLength(); j++) {
            final Element error = (Element) errors.item(j);
            toFinding(error, relativePath).ifPresent(findings::add);
        }
    }

    private Optional<Finding> toFinding(Element error, String relativePath) {
        final String checkName = extractCheckName(error.getAttribute("source"));
        final RuleMapping mapping = RULE_MAP.get(checkName);
        if (mapping == null) {
            return Optional.empty();
        }
        final int line = Integer.parseInt(error.getAttribute("line"));
        final String message = error.getAttribute("message");
        final Severity severity = xmlSeverityOrDefault(error.getAttribute("severity"), mapping.severity());
        return Optional.of(new Finding(
                mapping.code(), relativePath, line, line,
                message, severity, mapping.confidence(),
                TOOL_ID, checkName, Map.of(RULE_URL_KEY, mapping.ruleUrl())));
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve(REPORT_PATH);
    }

    String extractCheckName(String sourceFqn) {
        final int lastDot = sourceFqn.lastIndexOf('.');
        final String simpleName = lastDot >= 0 ? sourceFqn.substring(lastDot + 1) : sourceFqn;
        if (simpleName.endsWith(CHECK_SUFFIX)) {
            return simpleName.substring(0, simpleName.length() - CHECK_SUFFIX.length());
        }
        return simpleName;
    }

    private Severity xmlSeverityOrDefault(String xmlSeverity, Severity defaultSeverity) {
        return switch (xmlSeverity) {
            case "error" -> Severity.ERROR;
            case "info" -> Severity.INFO;
            case "warning" -> Severity.WARNING;
            default -> defaultSeverity;
        };
    }

    private static Map<String, RuleMapping> loadRuleMap() {
        final Properties properties = new Properties();
        try (InputStream stream = CheckstyleFindingSource.class.getResourceAsStream(RULE_MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + RULE_MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + RULE_MAPPING_RESOURCE, e);
        }
        final Map<String, RuleMapping> map = new HashMap<>();
        properties.forEach((key, value) -> map.put(key.toString(), parseRuleMapping(value.toString())));
        return Collections.unmodifiableMap(map);
    }

    private static RuleMapping parseRuleMapping(String value) {
        final String[] parts = value.split(FIELD_SEPARATOR);
        if (parts.length != 4) {
            throw new IllegalStateException("Invalid rule mapping format: " + value);
        }
        return new RuleMapping(
                HeuristicCode.valueOf(parts[0].trim()),
                Severity.valueOf(parts[1].trim()),
                Confidence.valueOf(parts[2].trim()),
                CHECKSTYLE_DOCS_BASE + parts[3].trim());
    }

    @FunctionalInterface
    private interface FileVisitor {
        void visit(Element fileElement, String absolutePath);
    }
}
