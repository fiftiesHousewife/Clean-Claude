package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.adapters.CheckstyleRuleMap.RuleMapping;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.fiftieshousewife.cleancode.core.Severity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckstyleFindingSource implements FindingSource {

    private static final Pattern LINE_LENGTH_FOUND = Pattern.compile("found (\\d+)");

    private final int lineLengthErrorThreshold;

    public CheckstyleFindingSource() {
        this(RecipeThresholds.defaults());
    }

    public CheckstyleFindingSource(final RecipeThresholds thresholds) {
        this.lineLengthErrorThreshold = thresholds.lineLengthErrorThreshold();
    }

    @Override
    public String id() {
        return "checkstyle";
    }

    @Override
    public String displayName() {
        return "Checkstyle";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        CheckstyleRuleMap.all().forEach(m -> codes.add(m.code()));
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
            return parseFindings(report, context);
        } catch (final Exception e) {
            throw new FindingSourceException("Failed to parse Checkstyle report: " + report, e);
        }
    }

    private List<Finding> parseFindings(final Path report, final ProjectContext context)
            throws FindingSourceException {
        final Document doc = XmlReportParser.parse(report);
        final List<Finding> findings = new ArrayList<>();
        final NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            final Element fileElement = (Element) fileNodes.item(i);
            final String relativePath = PathUtils.relativise(
                    fileElement.getAttribute("name"), context.projectRoot());
            addFileFindings(fileElement, relativePath, findings);
        }
        return findings;
    }

    private void addFileFindings(
            final Element fileElement, final String relativePath, final List<Finding> findings) {
        final NodeList errors = fileElement.getElementsByTagName("error");
        for (int j = 0; j < errors.getLength(); j++) {
            toFinding((Element) errors.item(j), relativePath).ifPresent(findings::add);
        }
    }

    private Optional<Finding> toFinding(final Element errorElement, final String relativePath) {
        final String checkName = extractCheckName(errorElement.getAttribute("source"));
        return CheckstyleRuleMap.lookup(checkName)
                .map(mapping -> buildFinding(errorElement, relativePath, checkName, mapping));
    }

    private Finding buildFinding(
            final Element errorElement, final String relativePath,
            final String checkName, final RuleMapping mapping) {
        final int line = Integer.parseInt(errorElement.getAttribute("line"));
        final String message = errorElement.getAttribute("message");
        final Severity severity = escalateLineLength(checkName, message,
                xmlSeverityOrDefault(errorElement.getAttribute("severity"), mapping.severity()));
        return new Finding(
                mapping.code(), relativePath, line, line,
                message, severity, mapping.confidence(),
                "checkstyle", checkName, Map.of("ruleUrl", mapping.ruleUrl()));
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("checkstyle/main.xml");
    }

    private String extractCheckName(final String sourceFqn) {
        final int lastDot = sourceFqn.lastIndexOf('.');
        final String simpleName = lastDot >= 0 ? sourceFqn.substring(lastDot + 1) : sourceFqn;
        return simpleName.endsWith("Check")
                ? simpleName.substring(0, simpleName.length() - "Check".length())
                : simpleName;
    }

    private Severity xmlSeverityOrDefault(final String xmlSeverity, final Severity defaultSeverity) {
        return switch (xmlSeverity) {
            case "error" -> Severity.ERROR;
            case "info" -> Severity.INFO;
            case "warning" -> Severity.WARNING;
            default -> defaultSeverity;
        };
    }

    private Severity escalateLineLength(final String checkName, final String message, final Severity current) {
        if (!"LineLength".equals(checkName)) {
            return current;
        }
        final Matcher matcher = LINE_LENGTH_FOUND.matcher(message);
        if (!matcher.find()) {
            return current;
        }
        final int actualLength = Integer.parseInt(matcher.group(1));
        return actualLength >= lineLengthErrorThreshold ? Severity.ERROR : current;
    }
}
