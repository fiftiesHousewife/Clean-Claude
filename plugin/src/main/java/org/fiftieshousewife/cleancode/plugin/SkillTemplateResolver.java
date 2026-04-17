package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class SkillTemplateResolver {

    private static final String TOKEN_CLASS_LINE_COUNT = "{{classLineCount}}";
    private static final String TOKEN_CLASS_TARGET_LINES = "{{classTargetLines}}";
    private static final String TOKEN_RECORD_COMPONENT_COUNT = "{{recordComponentCount}}";

    private static final int INDEX_CLASS_LINE_COUNT = 0;
    private static final int INDEX_CLASS_TARGET_LINES = 1;
    private static final int INDEX_RECORD_COMPONENT_COUNT = 2;

    private static final int LINE_COUNT_TO_TARGET_DIVISOR = 3;

    private final ThresholdsExtension thresholds;

    SkillTemplateResolver(final ThresholdsExtension thresholds) {
        this.thresholds = thresholds;
    }

    Optional<String> resolveCurrent(final String filename) {
        return loadTemplate(filename).map(this::replaceTokens);
    }

    Optional<String> resolveWithHash(final String filename, final String previousHash) {
        return loadTemplate(filename)
                .flatMap(template -> Optional.ofNullable(ThresholdsHashStore.parseHash(previousHash))
                        .map(values -> applyValues(template, values)));
    }

    String replaceTokens(final String content) {
        final int classLineCount = thresholds.getClassLineCount().get();
        final int classTargetLines = classLineCount / LINE_COUNT_TO_TARGET_DIVISOR;
        final int recordComponentCount = thresholds.getRecordComponentCount().get();
        return content
                .replace(TOKEN_CLASS_LINE_COUNT, String.valueOf(classLineCount))
                .replace(TOKEN_CLASS_TARGET_LINES, String.valueOf(classTargetLines))
                .replace(TOKEN_RECORD_COMPONENT_COUNT, String.valueOf(recordComponentCount));
    }

    private String applyValues(final String template, final String[] values) {
        return template
                .replace(TOKEN_CLASS_LINE_COUNT, values[INDEX_CLASS_LINE_COUNT])
                .replace(TOKEN_CLASS_TARGET_LINES, values[INDEX_CLASS_TARGET_LINES])
                .replace(TOKEN_RECORD_COMPONENT_COUNT, values[INDEX_RECORD_COMPONENT_COUNT]);
    }

    private Optional<String> loadTemplate(final String filename) {
        try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
            if (is == null) {
                return Optional.empty();
            }
            return Optional.of(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
