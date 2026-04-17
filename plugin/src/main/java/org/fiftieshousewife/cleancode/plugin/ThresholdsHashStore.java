package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.logging.Logger;

final class ThresholdsHashStore {

    private static final int HASH_PART_COUNT = 3;
    private static final int LINE_COUNT_TO_TARGET_DIVISOR = 3;

    private final ThresholdsExtension thresholds;
    private final Logger logger;

    ThresholdsHashStore(final ThresholdsExtension thresholds, final Logger logger) {
        this.thresholds = thresholds;
        this.logger = logger;
    }

    String computeCurrentHash() {
        final int classLineCount = thresholds.getClassLineCount().get();
        final int classTargetLines = classLineCount / LINE_COUNT_TO_TARGET_DIVISOR;
        final int recordComponentCount = thresholds.getRecordComponentCount().get();
        final String thresholdsHash =
                classLineCount + ":" + classTargetLines + ":" + recordComponentCount;
        return thresholdsHash;
    }

    static String[] parseHash(final String hash) {
        if (hash == null) {
            return null;
        }
        final String[] parts = hash.split(":");
        return parts.length == HASH_PART_COUNT ? parts : null;
    }

    String readHash(final Path hashFile) {
        try {
            if (Files.exists(hashFile)) {
                return Files.readString(hashFile).strip();
            }
        } catch (IOException e) {
            logger.warn("Could not read thresholds hash, will re-scaffold: {}", hashFile, e);
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
