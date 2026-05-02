package io.github.fiftieshousewife.cleancode.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Rules that exist behind upstream tools but are off by default because
 * they're noisy, opinionated, or only valuable to teams that have already
 * decided on the convention they enforce. Users opt them in via
 * {@code cleanCode.enabledOptionalRules = listOf("checkstyle:FinalLocalVariable")}.
 *
 * <p>Each entry is keyed by {@code tool + ":" + ruleRef}, matching the
 * {@code tool} and {@code ruleRef} fields on {@link Finding}.
 */
public final class OptionalRules {

    public record OptionalRule(String key, String code, String summary, String howToEnable) {}

    private static final Map<String, OptionalRule> DEFAULTS = buildDefaults();

    private static Map<String, OptionalRule> buildDefaults() {
        final Map<String, OptionalRule> map = new LinkedHashMap<>();
        register(map, "checkstyle:FinalLocalVariable", "G22",
                "Require all local variables to be declared final",
                "enabledOptionalRules += \"checkstyle:FinalLocalVariable\"");
        register(map, "pmd:UseLocaleWithCaseConversions", "G26",
                "Require an explicit Locale on String.toLowerCase / toUpperCase",
                "enabledOptionalRules += \"pmd:UseLocaleWithCaseConversions\"");
        return Map.copyOf(map);
    }

    private static void register(final Map<String, OptionalRule> map, final String key,
                                 final String code, final String summary, final String howToEnable) {
        map.put(key, new OptionalRule(key, code, summary, howToEnable));
    }

    public static Map<String, OptionalRule> defaults() {
        return DEFAULTS;
    }

    public static boolean isOptional(final Finding finding) {
        return DEFAULTS.containsKey(keyFor(finding));
    }

    public static String keyFor(final Finding finding) {
        return finding.tool() + ":" + finding.ruleRef();
    }

    public static boolean isEnabled(final Finding finding, final Set<String> enabledKeys) {
        if (!isOptional(finding)) {
            return true;
        }
        return enabledKeys.contains(keyFor(finding));
    }

    private OptionalRules() {}
}
