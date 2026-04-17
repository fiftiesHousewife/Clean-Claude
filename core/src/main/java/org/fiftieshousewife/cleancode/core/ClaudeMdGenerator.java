package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ClaudeMdGenerator {

    private ClaudeMdGenerator() {}

    public static void generate(final AggregatedReport report, final Path claudeMdFile,
                                final Path baselineFile) throws IOException {
        generate(report, claudeMdFile, baselineFile, List.of());
    }

    public static void generate(final AggregatedReport report, final Path claudeMdFile,
                                final Path baselineFile,
                                final List<String> dependencies) throws IOException {
        final Map<String, String> preservedAnnotations = ClaudeMdAnnotationParser.parseFromFile(claudeMdFile);
        final ClaudeMdSectionBuilder builder = new ClaudeMdSectionBuilder(report, preservedAnnotations);
        Files.writeString(claudeMdFile, builder.build(dependencies, baselineFile));
    }
}
