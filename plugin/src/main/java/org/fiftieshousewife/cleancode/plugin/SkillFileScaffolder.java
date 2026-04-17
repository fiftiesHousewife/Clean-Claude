package org.fiftieshousewife.cleancode.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.logging.Logger;

final class SkillFileScaffolder {

    private static final String SCAFFOLD_MANIFEST_RESOURCE = "/skills/scaffold-manifest.txt";
    private static final String THRESHOLDS_HASH_FILE = ".thresholds-hash";

    private final Path skillsDir;
    private final SkillTemplateResolver resolver;
    private final Logger logger;
    private final List<String> skillFiles;

    SkillFileScaffolder(final Path skillsDir,
            final ThresholdsExtension thresholds, final Logger logger) {
        this.skillsDir = skillsDir;
        this.resolver = new SkillTemplateResolver(thresholds);
        this.logger = logger;
        this.skillFiles = loadScaffoldManifest();
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

        if (thresholdsChanged) {
            skillFiles.forEach(filename -> scaffoldOrRefresh(filename, previousHash));
        } else {
            skillFiles.forEach(this::scaffoldIfMissing);
        }

        writeHashFile(hashFile, currentHash);
    }

    private void scaffoldIfMissing(final String filename) {
        final Path target = skillsDir.resolve(filename);
        if (Files.exists(target)) {
            return;
        }
        final String resolvedContent = resolver.loadAndResolveTemplate(filename);
        if (resolvedContent == null) {
            return;
        }
        writeSkillFile(target, resolvedContent);
    }

    private void scaffoldOrRefresh(final String filename, final String previousHash) {
        final Path target = skillsDir.resolve(filename);
        final String resolvedContent = resolver.loadAndResolveTemplate(filename);
        if (resolvedContent == null) {
            return;
        }
        if (!Files.exists(target)) {
            writeSkillFile(target, resolvedContent);
            return;
        }
        refreshIfUnmodified(target, filename, resolvedContent, previousHash);
    }

    private void refreshIfUnmodified(final Path target, final String filename,
            final String resolvedContent, final String previousHash) {
        final String currentFileContent;
        try {
            currentFileContent = Files.readString(target);
        } catch (IOException e) {
            throw new SkillScaffoldException(
                    "Could not read skill file for refresh: " + filename, e);
        }
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
    }

    private void writeSkillFile(final Path target, final String content) {
        try {
            final Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new SkillScaffoldException(
                    "Could not write skill file: " + target, e);
        }
        logger.lifecycle("Scaffolded skill file: {}", target);
    }

    private String readHashFile(final Path hashFile) {
        if (!Files.exists(hashFile)) {
            return null;
        }
        try {
            return Files.readString(hashFile).strip();
        } catch (IOException e) {
            throw new SkillScaffoldException(
                    "Could not read thresholds hash: " + hashFile, e);
        }
    }

    private void writeHashFile(final Path hashFile, final String hash) {
        try {
            Files.writeString(hashFile, hash);
        } catch (IOException e) {
            throw new SkillScaffoldException(
                    "Could not write thresholds hash: " + hashFile, e);
        }
    }

    private List<String> loadScaffoldManifest() {
        try (InputStream is = getClass().getResourceAsStream(SCAFFOLD_MANIFEST_RESOURCE)) {
            if (is == null) {
                throw new SkillScaffoldException(
                        "Scaffold manifest not found on classpath: " + SCAFFOLD_MANIFEST_RESOURCE,
                        null);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .collect(Collectors.toUnmodifiableList());
            }
        } catch (IOException e) {
            throw new SkillScaffoldException(
                    "Could not read scaffold manifest: " + SCAFFOLD_MANIFEST_RESOURCE, e);
        }
    }
}
