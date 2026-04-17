package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.logging.Logger;

final class ThresholdsHashStore {

    private final ThresholdsExtension thresholds;
    private final Logger logger;

    ThresholdsHashStore(final ThresholdsExtension thresholds, final Logger logger) {
        this.thresholds = thresholds;
        this.logger = logger;
    }

    String computeCurrentHash() {
        final int classLineCount = thresholds.getClassLineCount().get();
        final int classTargetLines = classLineCount / 3;
        final int recordComponentCount = thresholds.getRecordComponentCount().get();
        return classLineCount + ":" + classTargetLines + ":" + recordComponentCount;
    }

    String[] parseHash(final String hash) {
        if (hash == null) {
            return null;
        }
        final String[] parts = hash.split(":");
        if (parts.length != 3) {
            return null;
        }
        return parts;
    }

    String readHash(final Path hashFile) {
        try {
            if (Files.exists(hashFile)) {
                return Files.readString(hashFile).strip();
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    void writeHash(final Path hashFile, final String hash) {
        try {
            Files.writeString(hashFile, hash);
        } catch (IOException e) {
            logger.warn("Could not write thresholds hash: {}", hashFile, e);
        }
    }
}
