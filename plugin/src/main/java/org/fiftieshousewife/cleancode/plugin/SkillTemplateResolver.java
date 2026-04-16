package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SkillTemplateResolver {

    private final ThresholdsExtension thresholds;

    SkillTemplateResolver(final ThresholdsExtension thresholds) {
        this.thresholds = thresholds;
    }

    String loadAndResolveTemplate(final String filename) {
        final String template = loadTemplateResource(filename);
        if (template == null) {
            return null;
        }
        return replaceTokens(template);
    }

    String resolveTemplateWithHash(final String filename, final String previousHash) {
        if (previousHash == null) {
            return null;
        }
        final String template = loadTemplateResource(filename);
        if (template == null) {
            return null;
        }
        final String[] values = parseThresholdsHash(previousHash);
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

    String computeThresholdsHash() {
        final int classLineCount = thresholds.getClassLineCount().get();
        final int classTargetLines = classLineCount / 3;
        final int recordComponentCount = thresholds.getRecordComponentCount().get();
        return classLineCount + ":" + classTargetLines + ":" + recordComponentCount;
    }

    private String loadTemplateResource(final String filename) {
        try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String[] parseThresholdsHash(final String hash) {
        final String[] parts = hash.split(":");
        if (parts.length != 3) {
            return null;
        }
        return parts;
    }
}
