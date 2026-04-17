package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SkillTemplateResolver {

    private final ThresholdsExtension thresholds;
    private final ThresholdsHashStore hashStore;

    SkillTemplateResolver(final ThresholdsExtension thresholds,
            final ThresholdsHashStore hashStore) {
        this.thresholds = thresholds;
        this.hashStore = hashStore;
    }

    String resolveCurrent(final String filename) {
        final String template = loadTemplate(filename);
        if (template == null) {
            return null;
        }
        return replaceTokens(template);
    }

    String resolveWithHash(final String filename, final String previousHash) {
        if (previousHash == null) {
            return null;
        }
        final String template = loadTemplate(filename);
        if (template == null) {
            return null;
        }
        final String[] values = hashStore.parseHash(previousHash);
        if (values == null) {
            return null;
        }
        return template
                .replace("{{classLineCount}}", values[0])
                .replace("{{classTargetLines}}", values[1])
                .replace("{{recordComponentCount}}", values[2]);
    }

    String replaceTokens(final String content) {
        return content
                .replace("{{classLineCount}}", String.valueOf(thresholds.getClassLineCount().get()))
                .replace("{{classTargetLines}}",
                        String.valueOf(thresholds.getClassLineCount().get() / 3))
                .replace("{{recordComponentCount}}",
                        String.valueOf(thresholds.getRecordComponentCount().get()));
    }

    private String loadTemplate(final String filename) {
        try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
