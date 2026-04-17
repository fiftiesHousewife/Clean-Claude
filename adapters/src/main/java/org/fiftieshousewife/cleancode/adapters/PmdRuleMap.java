package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

final class PmdRuleMap {

    record Entry(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String RULE_MAPPING_RESOURCE = "/pmd/pmd-rule-mapping.properties";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final int FIELD_COUNT = 5;

    private static final int CODE_INDEX = 0;
    private static final int SEVERITY_INDEX = 1;
    private static final int CONFIDENCE_INDEX = 2;
    private static final int CATEGORY_INDEX = 3;
    private static final int SLUG_INDEX = 4;

    private static final Map<String, String> CATEGORY_URLS = Map.of(
            "BP", "https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#",
            "DS", "https://pmd.github.io/pmd/pmd_rules_java_design.html#",
            "EP", "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#",
            "CS", "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#");

    private PmdRuleMap() {}

    static Map<String, Entry> load() {
        final Properties properties = readProperties();
        final Map<String, Entry> mappings = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(rule ->
                mappings.put(rule, parseEntry(rule, properties.getProperty(rule))));
        return Collections.unmodifiableMap(mappings);
    }

    private static Properties readProperties() {
        final Properties properties = new Properties();
        try (InputStream stream = PmdRuleMap.class.getResourceAsStream(RULE_MAPPING_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("PMD rule mapping resource not found: " + RULE_MAPPING_RESOURCE);
            }
            properties.load(stream);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load PMD rule mapping: " + RULE_MAPPING_RESOURCE, e);
        }
        return properties;
    }

    static Entry parseEntry(final String rule, final String value) {
        final String[] parts = splitFields(rule, value);
        final HeuristicCode code = HeuristicCode.valueOf(parts[CODE_INDEX]);
        final Severity severity = Severity.valueOf(parts[SEVERITY_INDEX]);
        final Confidence confidence = Confidence.valueOf(parts[CONFIDENCE_INDEX]);
        final String categoryPrefix = categoryPrefix(rule, parts[CATEGORY_INDEX]);
        return new Entry(code, severity, confidence, categoryPrefix + parts[SLUG_INDEX]);
    }

    private static String[] splitFields(final String rule, final String value) {
        final String[] parts = value.split(FIELD_SEPARATOR);
        if (parts.length != FIELD_COUNT) {
            throw new IllegalStateException("Invalid PMD rule mapping for " + rule + ": " + value);
        }
        return parts;
    }

    private static String categoryPrefix(final String rule, final String category) {
        final String prefix = CATEGORY_URLS.get(category);
        if (prefix == null) {
            throw new IllegalStateException("Unknown PMD rule category for " + rule + ": " + category);
        }
        return prefix;
    }
}
