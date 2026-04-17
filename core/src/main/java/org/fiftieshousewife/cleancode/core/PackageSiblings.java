package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class PackageSiblings {

    private PackageSiblings() {}

    static List<String> findFor(final String sourceFile, final Path projectRoot) {
        if (projectRoot == null || sourceFile == null || !sourceFile.endsWith(".java")) {
            return List.of();
        }
        final Path absolute = projectRoot.resolve(sourceFile);
        final Path packageDir = absolute.getParent();
        if (packageDir == null || !Files.isDirectory(packageDir)) {
            return List.of();
        }
        final String thisFileName = absolute.getFileName().toString();
        try (var stream = Files.list(packageDir)) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".java"))
                    .filter(n -> !n.equals(thisFileName))
                    .sorted()
                    .map(n -> n.substring(0, n.length() - ".java".length()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
