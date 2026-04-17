package org.fiftieshousewife.cleancode.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

final class SkillFileScaffolder {

    private static final String SKILLS_MANIFEST_RESOURCE = "/skills-manifest.txt";
    private static final String THRESHOLDS_HASH_FILE = ".thresholds-hash";

    private final Path skillsDir;
    private final Logger logger;
    private final ThresholdsHashStore hashStore;
    private final SkillTemplateResolver templateResolver;
    private final List<String> skillFiles;

    SkillFileScaffolder(final Path skillsDir,
            final ThresholdsExtension thresholds, final Logger logger) {
        this.skillsDir = skillsDir;
        this.logger = logger;
        this.hashStore = new ThresholdsHashStore(thresholds, logger);
        this.templateResolver = new SkillTemplateResolver(thresholds);
        this.skillFiles = loadSkillsManifest();
    }

    void scaffold() {
        createSkillsDirectory();

        final String currentHash = hashStore.computeCurrentHash();
        final Path hashFile = skillsDir.resolve(THRESHOLDS_HASH_FILE);
        final String previousHash = hashStore.readHash(hashFile);

        if (currentHash.equals(previousHash)) {
            skillFiles.forEach(this::scaffoldMissingFile);
        } else {
            skillFiles.forEach(filename -> scaffoldChangedFile(filename, previousHash));
        }

        hashStore.writeHash(hashFile, currentHash);
    }

    private void createSkillsDirectory() {
        try {
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            throw new GradleException(
                    "Could not create skills directory: " + skillsDir, e);
        }
    }

    private void scaffoldMissingFile(final String filename) {
        final Path target = skillsDir.resolve(filename);
        if (Files.exists(target)) {
            return;
        }
        templateResolver.resolveCurrent(filename)
                .ifPresent(content -> writeSkillFile(target, content));
    }

    private void scaffoldChangedFile(final String filename, final String previousHash) {
        final Path target = skillsDir.resolve(filename);
        templateResolver.resolveCurrent(filename).ifPresent(resolvedContent -> {
            if (!Files.exists(target)) {
                writeSkillFile(target, resolvedContent);
            } else {
                refreshIfUnmodified(target, filename, resolvedContent, previousHash);
            }
        });
    }

    private void refreshIfUnmodified(final Path target, final String filename,
            final String resolvedContent, final String previousHash) {
        final String currentFileContent = readSkillFile(target, filename);
        final String previousTemplate = templateResolver.resolveWithHash(filename, previousHash)
                .orElse(null);
        if (previousTemplate != null && currentFileContent.equals(previousTemplate)) {
            writeSkillFile(target, resolvedContent);
            logger.lifecycle("Refreshed skill file with new thresholds: {}", target);
        } else {
            logger.warn("Threshold changed but {} has been customised — manual update needed",
                    filename);
        }
    }

    private String readSkillFile(final Path target, final String filename) {
        try {
            return Files.readString(target);
        } catch (IOException e) {
            throw new GradleException(
                    "Could not read skill file for refresh: " + filename, e);
        }
    }

    private void writeSkillFile(final Path target, final String content) {
        try {
            final Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content);
            logger.lifecycle("Scaffolded skill file: {}", target);
        } catch (IOException e) {
            throw new GradleException(
                    "Could not write skill file: " + target, e);
        }
    }

    private List<String> loadSkillsManifest() {
        try (InputStream is = getClass().getResourceAsStream(SKILLS_MANIFEST_RESOURCE)) {
            if (is == null) {
                throw new GradleException(
                        "Skills manifest resource not found: " + SKILLS_MANIFEST_RESOURCE);
            }
            final String contents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return contents.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new GradleException(
                    "Could not read skills manifest: " + SKILLS_MANIFEST_RESOURCE, e);
        }
    }
}
