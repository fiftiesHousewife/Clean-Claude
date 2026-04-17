package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class SkillTemplateResolver {

    private static final String CLASS_LINE_COUNT_TOKEN = "{{classLineCount}}";
    private static final String CLASS_TARGET_LINES_TOKEN = "{{classTargetLines}}";
    private static final String RECORD_COMPONENT_COUNT_TOKEN = "{{recordComponentCount}}";

    private static final String HASH_SEPARATOR = ":";
    private static final int EXPECTED_HASH_PARTS = 3;
    private static final int TARGET_LINES_DIVISOR = 3;

    private final ThresholdsExtension thresholds;

    SkillTemplateResolver(final ThresholdsExtension thresholds) {
        this.thresholds = thresholds;
    }

    String loadAndResolveTemplate(final String filename) {
        return loadTemplateResource(filename)
                .map(template -> replaceTokens(template, currentValues()))
                .orElse(null);
    }

    Optional<String> resolveTemplateWithHash(final String filename, final String previousHash) {
        return Optional.ofNullable(previousHash)
                .flatMap(ThresholdValues::parseHash)
                .flatMap(values -> loadTemplateResource(filename)
                        .map(template -> replaceTokens(template, values)));
    }

    String computeThresholdsHash() {
        return currentValues().toHash();
    }

    private ThresholdValues currentValues() {
        return ThresholdValues.from(thresholds, TARGET_LINES_DIVISOR);
    }

    private static String replaceTokens(final String content, final ThresholdValues values) {
        return content
                .replace(CLASS_LINE_COUNT_TOKEN, String.valueOf(values.classLineCount()))
                .replace(CLASS_TARGET_LINES_TOKEN, String.valueOf(values.classTargetLines()))
                .replace(RECORD_COMPONENT_COUNT_TOKEN, String.valueOf(values.recordComponentCount()));
    }

    private Optional<String> loadTemplateResource(final String filename) {
        try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
            if (is == null) {
                return Optional.empty();
            }
            return Optional.of(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    record ThresholdValues(int classLineCount, int classTargetLines, int recordComponentCount) {

        static ThresholdValues from(final ThresholdsExtension thresholds, final int targetLinesDivisor) {
            final int classLineCount = thresholds.getClassLineCount().get();
            final int recordComponentCount = thresholds.getRecordComponentCount().get();
            return new ThresholdValues(
                    classLineCount,
                    classLineCount / targetLinesDivisor,
                    recordComponentCount);
        }

        static Optional<ThresholdValues> parseHash(final String hash) {
            final String[] parts = hash.split(HASH_SEPARATOR);
            if (parts.length != EXPECTED_HASH_PARTS) {
                return Optional.empty();
            }
            try {
                return Optional.of(new ThresholdValues(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        String toHash() {
            final String colonSeparatedValues = classLineCount + HASH_SEPARATOR + classTargetLines
                    + HASH_SEPARATOR + recordComponentCount;
            return colonSeparatedValues;
        }
    }
}
