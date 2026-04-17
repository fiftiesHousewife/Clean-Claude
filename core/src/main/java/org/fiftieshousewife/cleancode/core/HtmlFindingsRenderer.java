package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.fiftieshousewife.cleancode.core.HtmlEscaping.escape;

final class HtmlFindingsRenderer {

    private static final String TEMPLATES_RESOURCE =
            "/org/fiftieshousewife/cleancode/core/html-findings-templates.properties";
    private static final Properties TEMPLATES = loadTemplates();

    private static final String SRC_JAVA_PREFIX = "src/main/java/";
    private static final String SRC_PREFIX = "src/";
    private static final char URL_SEPARATOR = '/';
    private static final String PROJECT_LOCATION = "(project)";

    private HtmlFindingsRenderer() {}

    static void appendFindingsByCode(final StringBuilder html, final List<Finding> findings,
                                      final String repositoryUrl) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> html.append(renderCodeGroup(entry.getKey(), entry.getValue(), repositoryUrl)));
    }

    static void appendToolSummary(final StringBuilder html, final List<Finding> findings) {
        final Map<String, Long> byTool = findings.stream()
                .collect(Collectors.groupingBy(Finding::tool, Collectors.counting()));

        final String toolRows = byTool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> renderToolRow(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining());

        html.append(template("tool-summary").replace("{toolRows}", toolRows));
    }

    static String renderCodeGroup(final HeuristicCode code, final List<Finding> group,
                                   final String repositoryUrl) {
        final String findingRows = group.stream()
                .sorted(Comparator.comparing(finding -> finding.sourceFile() != null ? finding.sourceFile() : ""))
                .map(finding -> renderFindingRow(finding, repositoryUrl))
                .collect(Collectors.joining());

        return template("code-group")
                .replace("{codeName}", escape(code.name()))
                .replace("{heuristicName}", escape(HeuristicDescriptions.name(code)))
                .replace("{count}", Integer.toString(group.size()))
                .replace("{reference}", renderOptionalParagraph("reference", HeuristicDescriptions.reference(code)))
                .replace("{guidance}", renderOptionalParagraph("guidance", HeuristicDescriptions.guidance(code)))
                .replace("{findingRows}", findingRows);
    }

    static String renderFindingRow(final Finding finding, final String repositoryUrl) {
        final String severityClass = "sev-" + finding.severity().name().toLowerCase(Locale.ROOT);
        final String location = formatLocation(finding);
        final String locationHtml = buildLocationHtml(finding, location, repositoryUrl);

        return template("finding-row")
                .replace("{severityClass}", severityClass)
                .replace("{severityName}", finding.severity().name())
                .replace("{locationHtml}", locationHtml)
                .replace("{message}", escape(finding.message()));
    }

    static String buildLocationHtml(final Finding finding, final String location, final String repositoryUrl) {
        if (cannotLinkToRepository(finding, repositoryUrl)) {
            return escape(location);
        }
        final String baseUrl = stripTrailingSlash(repositoryUrl);
        final String relativePath = substringFrom(finding.sourceFile(), SRC_PREFIX);
        final String fileUrl = baseUrl + URL_SEPARATOR + relativePath;
        final String linkedUrl = finding.startLine() > 0 ? fileUrl + "#L" + finding.startLine() : fileUrl;
        return template("location-link")
                .replace("{url}", escape(linkedUrl))
                .replace("{label}", escape(location));
    }

    static String formatLocation(final Finding finding) {
        if (finding.sourceFile() == null) {
            return PROJECT_LOCATION;
        }
        final String file = substringAfter(finding.sourceFile(), SRC_JAVA_PREFIX);
        return finding.startLine() > 0 ? file + ":" + finding.startLine() : file;
    }

    private static boolean cannotLinkToRepository(final Finding finding, final String repositoryUrl) {
        return finding.sourceFile() == null || repositoryUrl == null || repositoryUrl.isBlank();
    }

    private static String stripTrailingSlash(final String url) {
        if (!url.endsWith("/")) {
            return url;
        }
        final int lastCharIndex = url.length() - 1;
        return url.substring(0, lastCharIndex);
    }

    private static String substringFrom(final String text, final String marker) {
        final int markerIndex = text.indexOf(marker);
        return markerIndex < 0 ? text : text.substring(markerIndex);
    }

    private static String substringAfter(final String text, final String marker) {
        final int markerIndex = text.indexOf(marker);
        return markerIndex < 0 ? text : text.substring(markerIndex + marker.length());
    }

    private static String renderOptionalParagraph(final String cssClass, final String text) {
        if (text == null) {
            return "";
        }
        return template("optional-paragraph")
                .replace("{cssClass}", cssClass)
                .replace("{text}", escape(text));
    }

    private static String renderToolRow(final String tool, final Long count) {
        return template("tool-row")
                .replace("{tool}", escape(tool))
                .replace("{count}", Long.toString(count));
    }

    private static String template(final String key) {
        final String value = TEMPLATES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing HTML template fragment: " + key);
        }
        return value;
    }

    private static Properties loadTemplates() {
        final Properties properties = new Properties();
        try (InputStream stream = HtmlFindingsRenderer.class.getResourceAsStream(TEMPLATES_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing HTML templates resource: " + TEMPLATES_RESOURCE);
            }
            properties.load(stream);
            return properties;
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to load HTML templates: " + TEMPLATES_RESOURCE, exception);
        }
    }
}
