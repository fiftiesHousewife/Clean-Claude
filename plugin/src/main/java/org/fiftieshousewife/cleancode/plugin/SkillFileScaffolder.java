package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.logging.Logger;

final class SkillFileScaffolder {

    private static final List<String> SKILL_FILES = List.of(
            "SKILLS.md",
            "clean-code-exception-handling/SKILL.md",
            "clean-code-null-handling/SKILL.md",
            "clean-code-functions/SKILL.md",
            "clean-code-classes/SKILL.md",
            "clean-code-naming/SKILL.md",
            "clean-code-conditionals-and-expressions/SKILL.md",
            "clean-code-comments-and-clutter/SKILL.md",
            "clean-code-java-idioms/SKILL.md",
            "clean-code-test-quality/SKILL.md",
            "clean-code-project-conventions/SKILL.md");

    private static final String THRESHOLDS_HASH_FILE = ".thresholds-hash";

    private final Path skillsDir;
    private final SkillTemplateResolver resolver;
    private final Logger logger;

    SkillFileScaffolder(Path skillsDir,
            ThresholdsExtension thresholds, Logger logger) {
        this.skillsDir = skillsDir;
        this.resolver = new SkillTemplateResolver(thresholds);
        this.logger = logger;
    }

    void scaffold() {
        try {
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            logger.warn("Could not create skills directory: {}", skillsDir, e);
            return;
        }

        final String currentHash = resolver.computeThresholdsHash();
        final Path hashFile = skillsDir.resolve(THRESHOLDS_HASH_FILE);
        final String previousHash = readHashFile(hashFile);
        final boolean thresholdsChanged = !currentHash.equals(previousHash);

        SKILL_FILES.forEach(filename -> scaffoldFile(filename, thresholdsChanged, previousHash));

        writeHashFile(hashFile, currentHash);
    }

    private void scaffoldFile(String filename, boolean thresholdsChanged,
            String previousHash) {
        final Path target = skillsDir.resolve(filename);
        final String resolvedContent = resolver.loadAndResolveTemplate(filename);
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
            resolver.resolveTemplateWithHash(filename, previousHash)
                    .filter(currentFileContent::equals)
                    .ifPresentOrElse(
                            previousTemplate -> {
                                writeSkillFile(target, resolvedContent);
                                logger.lifecycle("Refreshed skill file with new thresholds: {}", target);
                            },
                            () -> logger.warn(
                                    "Threshold changed but {} has been customised — manual update needed",
                                    filename));
        } catch (IOException e) {
            logger.warn("Could not read skill file for refresh: {}", filename, e);
        }
    }

    private void writeSkillFile(Path target, String content) {
        try {
            final Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content);
            logger.lifecycle("Scaffolded skill file: {}", target);
        } catch (IOException e) {
            logger.warn("Could not write skill file: {}", target, e);
        }
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
