package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FixBriefGenerator {

    private static final String UNASSIGNED = "(project-level findings with no source file)";

    private FixBriefGenerator() {}

    public static List<Path> generate(final AggregatedReport report, final Path outputDir) throws IOException {
        return generate(report, outputDir, null);
    }

    public static List<Path> generate(final AggregatedReport report, final Path outputDir,
                                      final Path projectRoot) throws IOException {
        Files.createDirectories(outputDir);

        final Map<String, List<Finding>> byFile = groupByFile(report.findings());
        final List<Path> written = new ArrayList<>();

        for (final Map.Entry<String, List<Finding>> entry : byFile.entrySet()) {
            final Path brief = outputDir.resolve(briefFileName(entry.getKey()));
            Files.writeString(brief, BriefMarkdown.render(entry.getKey(), entry.getValue(), projectRoot));
            written.add(brief);
        }

        final Path indexFile = outputDir.resolve("_INDEX.md");
        Files.writeString(indexFile, BriefIndexMarkdown.render(
                report.projectName(), byFile, FixBriefGenerator::briefFileName));
        written.add(indexFile);

        return written;
    }

    private static Map<String, List<Finding>> groupByFile(final List<Finding> findings) {
        final Map<String, List<Finding>> byFile = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparing(FixBriefGenerator::fileKey)
                        .thenComparingInt(Finding::startLine))
                .forEach(f -> byFile.computeIfAbsent(fileKey(f), k -> new ArrayList<>()).add(f));
        return byFile;
    }

    private static String fileKey(final Finding f) {
        return f.sourceFile() == null ? UNASSIGNED : f.sourceFile();
    }

    private static String briefFileName(final String sourceFile) {
        if (UNASSIGNED.equals(sourceFile)) {
            return "project-level-findings.md";
        }
        final String base = sourceFile.replaceAll(".*/", "").replace(".java", "");
        return base + ".md";
    }
}
