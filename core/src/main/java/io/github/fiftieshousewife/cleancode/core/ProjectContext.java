package io.github.fiftieshousewife.cleancode.core;

import java.nio.file.Path;
import java.util.List;

public record ProjectContext(
        Path projectRoot,
        String projectName,
        String projectVersion,
        String javaVersion,
        List<Path> sourceRoots,
        List<Path> testSourceRoots,
        Path buildDir,
        Path reportsDir,
        List<String> dependencies
) {}
