package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.logging.Logger;

final class SkillFileScaffolder {

    private static final List<String> SKILL_FILES = List.of(
            "SKILLS.md",
            "exception-handling.md",
            "null-handling.md",
            "functions.md",
            "classes.md",
            "naming.md",
            "conditionals-and-expressions.md",
            "comments-and-clutter.md",
            "java-idioms.md",
            "project-conventions.md");

    private static final String THRESHOLDS_HASH_FILE = ".thresholds-hash";

    private final Path skillsDir;
    private final CleanCodeExtension.ThresholdsExtension thresholds;
    private final Logger logger;

    SkillFileScaffolder(Path skillsDir,
            CleanCodeExtension.ThresholdsExtension thresholds, Logger logger) {
        this.skillsDir = skillsDir;
        this.thresholds = thresholds;
        this.logger = logger;
    }

    void scaffold() {
        try {
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            logger.warn("Could not create skills directory: {}", skillsDir, e);
            return;
        }

        final String currentHash = computeThresholdsHash();
        final Path hashFile = skillsDir.resolve(THRESHOLDS_HASH_FILE);
        final String previousHash = readHashFile(hashFile);
        final boolean thresholdsChanged = !currentHash.equals(previousHash);

        SKILL_FILES.forEach(filename -> scaffoldFile(filename, thresholdsChanged, previousHash));

        writeHashFile(hashFile, currentHash);
    }

    private void scaffoldFile(String filename, boolean thresholdsChanged,
            String previousHash) {
        final Path target = skillsDir.resolve(filename);
        final String resolvedContent = loadAndResolveTemplate(filename);
        if (resolvedContent == null) {
            return;
        }

        if (!Files.exists(target)) {
            writeSkillFile(target, resolvedContent);
            return;
        }

        if (thresholdsChanged) {
            refreshIfUnmodified(target, filename, resolvedContent, previousHash);
        }
    }

    private void refreshIfUnmodified(Path target, String filename,
            String resolvedContent, String previousHash) {
        try {
            final String currentFileContent = Files.readString(target);
            final String previousTemplate = resolveTemplateWithHash(filename, previousHash);
            if (previousTemplate != null && currentFileContent.equals(previousTemplate)) {
                writeSkillFile(target, resolvedContent);
                logger.lifecycle("Refreshed skill file with new thresholds: {}", target);
            } else {
                logger.warn("Threshold changed but {} has been customised — manual update needed",
                        filename);
            }
        } catch (IOException e) {
            logger.warn("Could not read skill file for refresh: {}", filename, e);
        }
    }

    private String resolveTemplateWithHash(String filename, String previousHash) {
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

    private String loadAndResolveTemplate(String filename) {
        final String template = loadTemplateResource(filename);
        if (template == null) {
            return null;
        }
        return replaceTokens(template);
    }

    private String loadTemplateResource(String filename) {
        try (InputStream is = getClass().getResourceAsStream("/skills/" + filename)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeSkillFile(Path target, String content) {
        try {
            Files.writeString(target, content);
            logger.lifecycle("Scaffolded skill file: {}", target);
        } catch (IOException e) {
            logger.warn("Could not write skill file: {}", target, e);
        }
    }

    String replaceTokens(String content) {
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

    private String[] parseThresholdsHash(String hash) {
        final String[] parts = hash.split(":");
        if (parts.length != 3) {
            return null;
        }
        return parts;
    }

    private String readHashFile(Path hashFile) {
        try {
            if (Files.exists(hashFile)) {
                return Files.readString(hashFile).strip();
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void writeHashFile(Path hashFile, String hash) {
        try {
            Files.writeString(hashFile, hash);
        } catch (IOException e) {
            logger.warn("Could not write thresholds hash: {}", hashFile, e);
        }
    }
}
